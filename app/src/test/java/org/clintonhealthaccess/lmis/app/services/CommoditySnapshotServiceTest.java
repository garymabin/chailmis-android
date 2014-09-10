/*
 * Copyright (c) 2014, Thoughtworks Inc
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the FreeBSD Project.
 */

package org.clintonhealthaccess.lmis.app.services;

import android.content.Context;

import com.google.inject.Inject;

import org.clintonhealthaccess.lmis.app.models.Category;
import org.clintonhealthaccess.lmis.app.models.Commodity;
import org.clintonhealthaccess.lmis.app.models.CommodityActivity;
import org.clintonhealthaccess.lmis.app.models.CommoditySnapshot;
import org.clintonhealthaccess.lmis.app.models.DataSet;
import org.clintonhealthaccess.lmis.app.models.Dispensing;
import org.clintonhealthaccess.lmis.app.models.DispensingItem;
import org.clintonhealthaccess.lmis.app.models.User;
import org.clintonhealthaccess.lmis.app.models.api.DataValueSet;
import org.clintonhealthaccess.lmis.app.persistence.DbUtil;
import org.clintonhealthaccess.lmis.utils.LMISTestCase;
import org.clintonhealthaccess.lmis.utils.RobolectricGradleTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.clintonhealthaccess.lmis.app.utils.ViewHelpers.getID;
import static org.clintonhealthaccess.lmis.utils.TestInjectionUtil.setUpInjection;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(RobolectricGradleTestRunner.class)
public class CommoditySnapshotServiceTest extends LMISTestCase {
    public static final String DISPENSING = "DISPENSING";
    @Inject
    CommoditySnapshotService commoditySnapshotService;

    @Inject
    CommodityService commodityService;

    @Inject
    DbUtil dbUtil;
    private GenericDao<Category> categoryDao;
    private GenericDao<Commodity> commodityDao;
    private GenericDao<CommodityActivity> commodityActivityGenericDao;
    private GenericDao<CommoditySnapshot> snapshotDao;
    private GenericDao<DataSet> dataSetGenericDao;


    @Before
    public void setUp() {
        Context context = Robolectric.application;
        categoryDao = new GenericDao<>(Category.class, context);
        commodityDao = new GenericDao<>(Commodity.class, context);
        snapshotDao = new GenericDao<>(CommoditySnapshot.class, context);
        commodityActivityGenericDao = new GenericDao<>(CommodityActivity.class, context);
        dataSetGenericDao = new GenericDao<>(DataSet.class, context);
        setUpInjection(this);
        generateTestCommodities();
    }

    @Test
    public void shouldCreateNewDailyCommoditySnapshotIfNotExist() throws SQLException {


        Commodity fetchedCommodity1 = commodityDao.queryForAll().get(0);
        Commodity fetchedCommodity2 = commodityDao.queryForAll().get(1);

        Dispensing dispensing = new Dispensing(false);
        DispensingItem snapshotable = new DispensingItem(fetchedCommodity1, 3);
        DispensingItem snapshotable1 = new DispensingItem(fetchedCommodity2, 4);
        snapshotable.setDispensing(dispensing);
        snapshotable1.setDispensing(dispensing);
        commoditySnapshotService.add(snapshotable);
        commoditySnapshotService.add(snapshotable1);

        List<CommoditySnapshot> commoditySnapshots = snapshotDao.queryForAll();

        assertThat(commoditySnapshots.size(), is(2));
        assertThat(commoditySnapshots.get(0).getValue(), is("3"));
    }

    @Test
    public void shouldUpdateDailyCommoditySummaryIfItExists() throws Exception {
        Commodity fetchedCommodity = commodityDao.queryForAll().get(0);

        DispensingItem dispensingItem = new DispensingItem(fetchedCommodity, 3);
        Dispensing dispensing = new Dispensing(false);
        dispensingItem.setDispensing(dispensing);
        commoditySnapshotService.add(dispensingItem);
        commoditySnapshotService.add(dispensingItem);
        commoditySnapshotService.add(dispensingItem);

        List<CommoditySnapshot> commoditySnapshots = snapshotDao.queryForAll();
        assertThat(commoditySnapshots.size(), is(1));
        assertThat(commoditySnapshots.get(0).getValue(), is("9"));
    }

    @Test
    public void shouldMarkSyncedItemAsUnSyncedWhenAnUpdateOccurs() throws Exception {

        Commodity fetchedCommodity = commodityDao.queryForAll().get(0);
        DispensingItem dispensingItem = new DispensingItem(fetchedCommodity, 3);
        Dispensing dispensing = new Dispensing(false);
        dispensingItem.setDispensing(dispensing);

        CommoditySnapshot commoditySnapshot = new CommoditySnapshot(fetchedCommodity, dispensingItem.getActivitiesValues().get(0).getActivity(), "3");
        commoditySnapshot.setSynced(true);
        snapshotDao.create(commoditySnapshot);
        commoditySnapshotService.add(dispensingItem);

        List<CommoditySnapshot> commoditySnapshots = snapshotDao.queryForAll();
        assertThat(commoditySnapshots.size(), is(1));
        assertThat(commoditySnapshots.get(0).isSynced(), is(false));
    }

    @Test
    public void shouldGetCommoditySnapshotsWithSyncedAsFalse() {

        Commodity fetchedCommodity1 = commodityDao.queryForAll().get(0);
        Commodity fetchedCommodity2 = commodityDao.queryForAll().get(1);
        DispensingItem dispensingItem = new DispensingItem(fetchedCommodity1, 3);
        Dispensing dispensing = new Dispensing(false);
        dispensingItem.setDispensing(dispensing);

        CommoditySnapshot commoditySnapshot = new CommoditySnapshot(fetchedCommodity1, dispensingItem.getActivitiesValues().get(0).getActivity(), "3");
        commoditySnapshot.setSynced(true);
        snapshotDao.create(commoditySnapshot);

        DispensingItem snapshotable = new DispensingItem(fetchedCommodity2, 4);
        snapshotable.setDispensing(dispensing);
        commoditySnapshotService.add(snapshotable);

        List<CommoditySnapshot> unsynchedSnapshots = commoditySnapshotService.getUnSyncedSnapshots();
        assertThat(unsynchedSnapshots.size(), is(1));
    }

    @Test
    public void shouldConvertSnapshotsToDataValueSets() throws Exception {
        Commodity fetchedCommodity1 = commodityDao.queryForAll().get(0);
        Commodity fetchedCommodity2 = commodityDao.queryForAll().get(1);

        List<CommodityActivity> commodityActivities = new ArrayList<>(fetchedCommodity1.getCommodityActivitiesSaved());
        List<CommodityActivity> commodityActivities1 = new ArrayList<>(fetchedCommodity2.getCommodityActivitiesSaved());
        CommodityActivity commodityActivity = commodityActivities.get(0);
        CommoditySnapshot snapshot1 = new CommoditySnapshot(fetchedCommodity1, commodityActivity, "3");
        CommoditySnapshot snapshot2 = new CommoditySnapshot(fetchedCommodity2, commodityActivities1.get(0), "8");
        List<CommoditySnapshot> snapshots = Arrays.asList(snapshot1, snapshot2);

        DataValueSet valueSet = commoditySnapshotService.getDataValueSetFromSnapshots(snapshots, "orgUnit");

        assertThat(valueSet, is(notNullValue()));
        assertThat(valueSet.getDataValues().size(), is(2));
        assertThat(valueSet.getDataValues().get(0).getValue(), is("3"));
        assertThat(valueSet.getDataValues().get(1).getValue(), is("8"));
        assertThat(valueSet.getDataValues().get(0).getDataElement(), is(commodityActivity.getId()));
    }

    @Test
    public void shouldMarkSnapshotsAsSyncedIfSyncIsSuccessful() throws Exception {
        setUpSuccessHttpPostRequest(200, "successfulSnapshotPush.json");
        Commodity fetchedCommodity1 = commodityDao.queryForAll().get(0);
        Commodity fetchedCommodity2 = commodityDao.queryForAll().get(1);

        List<CommodityActivity> commodityActivities = new ArrayList<>(fetchedCommodity1.getCommodityActivitiesSaved());
        List<CommodityActivity> commodityActivities1 = new ArrayList<>(fetchedCommodity2.getCommodityActivitiesSaved());
        CommodityActivity commodityActivity = commodityActivities.get(0);
        CommoditySnapshot snapshot1 = new CommoditySnapshot(fetchedCommodity1, commodityActivity, "3");
        CommoditySnapshot snapshot2 = new CommoditySnapshot(fetchedCommodity2, commodityActivities1.get(0), "8");
        snapshotDao.create(snapshot1);
        snapshotDao.create(snapshot2);

        assertThat(commoditySnapshotService.getUnSyncedSnapshots().size(), is(2));

        commoditySnapshotService.syncWithServer(new User("user", "user"));

        assertThat(commoditySnapshotService.getUnSyncedSnapshots().size(), is(0));


    }

    @Test
    public void shouldNotMarkSnapshotsAsSyncedIfSyncFails() throws Exception {

        setUpSuccessHttpPostRequest(200, "failureSnapshotPush.json");
        Commodity fetchedCommodity1 = commodityDao.queryForAll().get(0);
        Commodity fetchedCommodity2 = commodityDao.queryForAll().get(1);

        List<CommodityActivity> commodityActivities = new ArrayList<>(fetchedCommodity1.getCommodityActivitiesSaved());
        List<CommodityActivity> commodityActivities1 = new ArrayList<>(fetchedCommodity2.getCommodityActivitiesSaved());
        CommodityActivity commodityActivity = commodityActivities.get(0);
        CommoditySnapshot snapshot1 = new CommoditySnapshot(fetchedCommodity1, commodityActivity, "3");
        CommoditySnapshot snapshot2 = new CommoditySnapshot(fetchedCommodity2, commodityActivities1.get(0), "8");
        snapshotDao.create(snapshot1);
        snapshotDao.create(snapshot2);

        assertThat(commoditySnapshotService.getUnSyncedSnapshots().size(), is(2));

        commoditySnapshotService.syncWithServer(new User("user", "user"));

        assertThat(commoditySnapshotService.getUnSyncedSnapshots().size(), is(2));
    }

    private void generateTestCommodities() {
        DataSet dataSet = new DataSet("123123");
        dataSet.setPeriodType("Daily");
        dataSetGenericDao.createOrUpdate(dataSet);
        Category category = new Category("commodities");
        categoryDao.create(category);

        Commodity commodity = new Commodity("Panado", category);
        Commodity commodity2 = new Commodity("other drug", category);
        commodityDao.create(commodity);
        commodityDao.create(commodity2);
        CommodityActivity activity = new CommodityActivity(commodity, getID(), "Panado_DISPENSING", DispensingItem.DISPENSE);
        activity.setDataSet(dataSet);
        commodityActivityGenericDao.create(activity);
        CommodityActivity activity2 = new CommodityActivity(commodity2, getID(), "other drug_DISPENSING", DispensingItem.DISPENSE);
        activity2.setDataSet(dataSet);
        commodityActivityGenericDao.create(activity2);
    }


}
