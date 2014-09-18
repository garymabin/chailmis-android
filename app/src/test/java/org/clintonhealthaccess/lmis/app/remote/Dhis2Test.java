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

package org.clintonhealthaccess.lmis.app.remote;

import com.google.inject.Inject;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.clintonhealthaccess.lmis.app.models.Category;
import org.clintonhealthaccess.lmis.app.models.Commodity;
import org.clintonhealthaccess.lmis.app.models.CommodityActionValue;
import org.clintonhealthaccess.lmis.app.models.OrderReason;
import org.clintonhealthaccess.lmis.app.models.OrderType;
import org.clintonhealthaccess.lmis.app.models.User;
import org.clintonhealthaccess.lmis.app.services.CategoryService;
import org.clintonhealthaccess.lmis.app.services.CommodityService;
import org.clintonhealthaccess.lmis.utils.LMISTestCase;
import org.clintonhealthaccess.lmis.utils.RobolectricGradleTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.clintonhealthaccess.lmis.utils.TestInjectionUtil.setUpInjection;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.robolectric.Robolectric.getSentHttpRequest;

@RunWith(RobolectricGradleTestRunner.class)
public class Dhis2Test extends LMISTestCase {

    @Inject
    private Dhis2 dhis2;

    @Inject
    private CommodityService commodityService;

    @Inject
    private CategoryService categoryService;


    @Before
    public void setUp() throws Exception {
        setUpInjection(this);
    }


    @Test
    public void testShouldValidateUserLogin() throws Exception {
        setUpSuccessHttpGetRequest(200, "userResponse.json");

        User user = new User("test", "pass");
        dhis2.validateLogin(user);

        HttpRequest lastSentHttpRequest = getSentHttpRequest(0);
        assertThat(lastSentHttpRequest.getRequestLine().getUri(), equalTo(dhis2BaseUrl + "/api/me"));
        Header authorizationHeader = lastSentHttpRequest.getFirstHeader("Authorization");
        assertThat(authorizationHeader.getValue(), equalTo("Basic dGVzdDpwYXNz"));
    }

    @Test
    public void testShouldFetchReasonsForOrder() throws Exception {
        setUpSuccessHttpGetRequest(200, "systemSettingForReasonsForOrder.json");
        List<String> reasons = dhis2.fetchOrderReasons(new User("test", "pass"));
        assertThat(reasons.size(), is(3));
        assertThat(reasons, contains(OrderReason.HIGH_DEMAND, OrderReason.LOSSES, OrderReason.EXPIRIES));
    }


    @Test
    public void testShouldFetchOrderTypes() throws Exception {
        setUpSuccessHttpGetRequest(200, "orderTypes.json");
        List<OrderType> orderTypes = dhis2.fetchOrderTypes(new User("test", "pass"));
        assertThat(orderTypes.size(), is(2));
    }

    @Test
    public void shouldFetchCommoditiesFromAPIServiceEndPoint() throws Exception {
        setUpSuccessHttpGetRequest(200, "dataSets.json");
        List<Category> categories = dhis2.fetchCommodities(new User());
        String commodityName = "Cotrimoxazole_suspension";
        assertThat(categories.size(), is(10));
        Category category = categories.get(0);
        assertThat(category.getNotSavedCommodities().size(), is(greaterThan(1)));
        assertThat(category.getName(), is("Antibiotics"));
        assertThat(category.getNotSavedCommodities().get(0).getName(), is(commodityName));
    }

    @Test
    public void shouldCreateCommodityActionValueForEachDataValue() throws Exception {
        String orgUnit = "orgnunit";
        User user = new User();
        user.setFacilityCode(orgUnit);

        setUpSuccessHttpGetRequest(200, "dataSets.json");
        setUpSuccessHttpGetRequest(200, "dataValues.json");

        commodityService.saveToDatabase(dhis2.fetchCommodities(user));
        categoryService.clearCache();
        List<Commodity> commodities = commodityService.all();
        assertThat(commodities.size(), greaterThan(0));
        List<CommodityActionValue> result = dhis2.fetchCommodityActionValues(commodities, user);
        assertThat(result.size(), is(210));
        assertThat(result.get(0).getValue(), is("469"));
        assertThat(result.get(0).getPeriod(), is("20131229"));
        assertThat(result.get(0).getCommodityAction().getId(), is("f5edb97ceca"));
    }


    @Test
    public void shouldSearchConstantsForMonthlyStockCountDay() throws Exception {
        setUpSuccessHttpGetRequest(200, "constants.json");
        Integer day = dhis2.getDayForMonthlyStockCount(new User());
        assertThat(day, is(20));
    }

    @Test
    public void shouldFallBackToDefaultIfNoContantsAreAvailable() throws Exception {
        setUpSuccessHttpGetRequest(200, "constantsEmpty.json");
        Integer day = dhis2.getDayForMonthlyStockCount(new User());
        assertThat(day, is(24));
    }
}