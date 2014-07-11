package org.clintonhealthaccess.lmis.app.fragments;

import android.app.Dialog;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

import org.clintonhealthaccess.lmis.app.R;
import org.clintonhealthaccess.lmis.app.activities.viewModels.CommodityViewModel;
import org.clintonhealthaccess.lmis.app.models.Category;
import org.clintonhealthaccess.lmis.app.models.Commodity;
import org.clintonhealthaccess.lmis.app.services.CategoryService;
import org.clintonhealthaccess.lmis.app.services.StockService;
import org.clintonhealthaccess.lmis.utils.RobolectricGradleTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowDialog;

import java.util.ArrayList;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.clintonhealthaccess.lmis.utils.TestFixture.initialiseDefaultCommodities;
import static org.clintonhealthaccess.lmis.utils.TestInjectionUtil.setUpInjection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.application;
import static org.robolectric.util.FragmentTestUtil.startFragment;

@RunWith(RobolectricGradleTestRunner.class)
public class ItemSelectFragmentTest {
    @Inject
    private CategoryService categoryService;

    private ItemSelectFragment itemSelectFragment;
    private StockService mockStockService;

    @Before
    public void setUp() throws Exception {
        mockStockService = mock(StockService.class);
        setUpInjection(this, new AbstractModule() {
            @Override
            protected void configure() {
                bind(StockService.class).toInstance(mockStockService);
            }
        });
        when(mockStockService.getStockLevelFor((Commodity)anyObject())).thenReturn(10);

        initialiseDefaultCommodities(application);

        Category antiMalarialCategory = categoryService.all().get(0);
        itemSelectFragment = ItemSelectFragment.newInstance(antiMalarialCategory, new ArrayList<CommodityViewModel>());
        startFragment(itemSelectFragment);
    }

    @Test
    public void testShouldRenderAllCategoryButtons() throws Exception {
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertTrue(dialog.isShowing());

        LinearLayout categoriesLayout = (LinearLayout) dialog.findViewById(R.id.itemSelectOverlayCategories);
        assertThat(categoriesLayout, not(nullValue()));

        assertThat(categoriesLayout.getChildCount(), is(6));
        for (int i = 0; i < categoriesLayout.getChildCount(); i++) {
            Button button = (Button)categoriesLayout.getChildAt(i);
            Category currentCategory = categoryService.all().get(i);
            assertThat(button.getText().toString(), equalTo(currentCategory.getName()));
        }
    }

    @Test
    public void testCategoryButtonClickChangesCommoditiesShowing() throws Exception {
        Dialog dialog = ShadowDialog.getLatestDialog();
        LinearLayout categoriesLayout = (LinearLayout) dialog.findViewById(R.id.itemSelectOverlayCategories);

        Button secondCategoryButton = (Button) categoriesLayout.getChildAt(1);
        secondCategoryButton.performClick();

        ListView commoditiesLayout = (ListView) dialog.findViewById(R.id.listViewCommodities);
        assertThat(commoditiesLayout, not(nullValue()));
        assertThat(commoditiesLayout.getAdapter().getCount(), is(1));

        assertThat(secondCategoryButton.isSelected(), is(true));
    }

    @Test
    public void testCloseButtonExists() throws Exception {
        Dialog dialog = ShadowDialog.getLatestDialog();
        Button buttonClose = (Button) dialog.findViewById(R.id.buttonClose);
        assertThat(buttonClose, not(nullValue()));
    }

    @Test
    public void testCloseButtonClosesTheDialog() throws Exception {
        Dialog dialog = ShadowDialog.getLatestDialog();
        Button buttonClose = (Button) dialog.findViewById(R.id.buttonClose);
        assertTrue(itemSelectFragment.isVisible());
        buttonClose.callOnClick();
        assertFalse(itemSelectFragment.isVisible());

    }
}