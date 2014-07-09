package org.clintonhealthaccess.lmis.app.activities;

import android.support.v4.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.inject.Inject;

import org.clintonhealthaccess.lmis.app.R;
import org.clintonhealthaccess.lmis.app.adapters.SelectedCommoditiesAdapter;
import org.clintonhealthaccess.lmis.app.events.CommodityToggledEvent;
import org.clintonhealthaccess.lmis.app.fragments.DispenseConfirmationFragment;
import org.clintonhealthaccess.lmis.app.fragments.ItemSelectFragment;
import org.clintonhealthaccess.lmis.app.models.Category;
import org.clintonhealthaccess.lmis.app.models.Commodity;
import org.clintonhealthaccess.lmis.app.models.Dispensing;
import org.clintonhealthaccess.lmis.app.persistence.CommoditiesRepository;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import roboguice.inject.InjectView;

public class DispenseActivity extends BaseActivity {

    @Inject
    private CommoditiesRepository commoditiesRepository;

    @InjectView(R.id.listViewSelectedCommodities)
    protected ListView listViewSelectedCommodities;
    @InjectView(R.id.buttonSubmitDispense)
    Button buttonSubmitDispense;


    protected SelectedCommoditiesAdapter selectedCommoditiesAdapter;
    protected ArrayList<Commodity> selectedCommodities = new ArrayList<>();
    private List<Dispensing> dispensings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dispense);

        LinearLayout categoriesLayout = (LinearLayout) findViewById(R.id.layoutCategories);

        List<Category> categoryList = commoditiesRepository.allCategories();

        Drawable drawable = getResources().getDrawable(R.drawable.arrow_black_right);

        drawable.setBounds(0, 0, 20, 30);

        for (final Category category : categoryList) {
            Button button = new Button(this);

            button.setBackgroundResource(R.drawable.category_button_on_overlay);

            button.setCompoundDrawables(null, null, drawable, null);

            button.setText(category.getName());
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentManager fm = getSupportFragmentManager();
                    ItemSelectFragment dialog = ItemSelectFragment.newInstance(category, selectedCommodities);
                    dialog.show(fm, "selectCommodities");
                }
            });
            categoriesLayout.addView(button);
        }

        buttonSubmitDispense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmDispensing();
            }
        });

        selectedCommoditiesAdapter = new SelectedCommoditiesAdapter(this, R.layout.commodity_list_item, new ArrayList<Commodity>());
        listViewSelectedCommodities.setAdapter(selectedCommoditiesAdapter);
        EventBus.getDefault().register(this);
    }

    private void confirmDispensing() {
        FragmentManager fm = getSupportFragmentManager();
        DispenseConfirmationFragment dialog = DispenseConfirmationFragment.newInstance(new Dispensing());
        dialog.show(fm, "confirmDispensing");
    }

    public void onEvent(CommodityToggledEvent event) {
        Commodity commodity = event.getCommodity();
        if (selectedCommodities.contains(commodity)) {
            selectedCommodities.remove(commodity);
            selectedCommoditiesAdapter.remove(commodity);
        } else {
            selectedCommoditiesAdapter.add(commodity);
            selectedCommodities.add(commodity);
        }
        selectedCommoditiesAdapter.notifyDataSetChanged();
    }

}
