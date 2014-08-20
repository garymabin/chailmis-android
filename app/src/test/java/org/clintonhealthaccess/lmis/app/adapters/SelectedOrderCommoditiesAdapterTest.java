/*
 * Copyright (c) 2014, Clinton Health Access Initiative
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

package org.clintonhealthaccess.lmis.app.adapters;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.clintonhealthaccess.lmis.app.R;
import org.clintonhealthaccess.lmis.app.activities.viewmodels.OrderCommodityViewModel;
import org.clintonhealthaccess.lmis.app.models.Commodity;
import org.clintonhealthaccess.lmis.app.models.OrderReason;
import org.clintonhealthaccess.lmis.app.models.StockItem;
import org.clintonhealthaccess.lmis.utils.RobolectricGradleTestRunner;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowAbsSpinner;
import org.robolectric.shadows.ShadowDatePickerDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowHandler;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static java.util.Calendar.DAY_OF_MONTH;
import static org.clintonhealthaccess.lmis.app.adapters.SelectedOrderCommoditiesAdapter.MIN_ORDER_PERIOD;
import static org.clintonhealthaccess.lmis.app.adapters.SelectedOrderCommoditiesAdapter.SIMPLE_DATE_FORMAT;
import static org.clintonhealthaccess.lmis.utils.ListTestUtils.getViewFromListRow;
import static org.clintonhealthaccess.lmis.utils.TestInjectionUtil.setUpInjection;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class SelectedOrderCommoditiesAdapterTest {

    public static final String ROUTINE = "Routine";
    private SelectedOrderCommoditiesAdapter adapter;
    private int list_item_layout = R.layout.selected_order_commodity_list_item;
    private List<OrderReason> orderReasons = new ArrayList<>();
    private OrderReason emergency = new OrderReason("Emergency", OrderReason.ORDER_REASONS_JSON_KEY);
    private OrderReason routine = new OrderReason(ROUTINE, OrderReason.ORDER_REASONS_JSON_KEY);
    private OrderCommodityViewModel commodityViewModel;
    private ArrayList<OrderCommodityViewModel> commodities;

    @Before
    public void setUp() {
        setUpInjection(this);
        Commodity commodity = mock(Commodity.class);
        when(commodity.getName()).thenReturn("Aspirin");
        when(commodity.getOrderDuration()).thenReturn(30);
        when(commodity.getStockItem()).thenReturn(new StockItem(commodity, 20));

        commodities = new ArrayList<>();
        commodityViewModel = mock(OrderCommodityViewModel.class);
        when(commodityViewModel.getCommodity()).thenReturn(commodity);
        when(commodityViewModel.getOrderReasonPosition()).thenReturn(0);
        commodities.add(commodityViewModel);

        orderReasons.add(emergency);
        orderReasons.add(routine);

        adapter = new SelectedOrderCommoditiesAdapter(Robolectric.application, list_item_layout, commodities, orderReasons);
    }

    @Test
    public void shouldShowDateDialogWhenStartDateTextFieldIsClicked() {
        TextView textViewStartDate = (TextView) getViewFromListRow(adapter, list_item_layout, R.id.textViewStartDate);
        textViewStartDate.performClick();

        Dialog dateDialog = ShadowDialog.getLatestDialog();
        assertNotNull(dateDialog);
    }

    @Test
    public void shouldShowDateDialogWhenEndDateEditTextIsClicked() throws Exception {
        TextView textViewEndDate = (TextView) getViewFromListRow(adapter, list_item_layout, R.id.textViewEndDate);
        textViewEndDate.performClick();

        Dialog dateDialog = ShadowDialog.getLatestDialog();
        assertNotNull(dateDialog);
    }

    @Test
    public void shouldNotBeAbleToSetTheEndDateEarlierThanStartDate() throws Exception {
        ViewGroup genericLayout = new LinearLayout(Robolectric.application);

        View convertView = LayoutInflater.from(Robolectric.application).inflate(list_item_layout, null);

        View rowView = adapter.getView(0, convertView, genericLayout);

        TextView textViewStartDate = (TextView) rowView.findViewById(R.id.textViewStartDate);

        TextView textViewEndDate = (TextView) rowView.findViewById(R.id.textViewEndDate);

        Calendar calendarStartDate = Calendar.getInstance();

        calendarStartDate.add(DAY_OF_MONTH, 50);

        String startDateAsText = SIMPLE_DATE_FORMAT.format(calendarStartDate.getTime());

        textViewStartDate.setText(startDateAsText);

        assertThat(((TextView) rowView.findViewById(R.id.textViewStartDate)).getText().toString(), is(startDateAsText));

        textViewEndDate.performClick();

        DatePickerDialog dateDialog = (DatePickerDialog) ShadowDatePickerDialog.getLatestDialog();

        calendarStartDate.add(DAY_OF_MONTH, MIN_ORDER_PERIOD);

        Date minDate = new Date(dateDialog.getDatePicker().getMinDate());

        String minEndDateAsText = SIMPLE_DATE_FORMAT.format(minDate);

        startDateAsText = SIMPLE_DATE_FORMAT.format(calendarStartDate.getTime());

        assertThat(minEndDateAsText, is(startDateAsText));
    }

    @Test
    public void shouldNotBeAbleToSetStartDateGreaterThanEndDate() throws Exception {
        ViewGroup genericLayout = new LinearLayout(Robolectric.application);

        View convertView = LayoutInflater.from(Robolectric.application).inflate(list_item_layout, null);

        View rowView = adapter.getView(0, convertView, genericLayout);

        TextView textViewStartDate = (TextView) rowView.findViewById(R.id.textViewStartDate);

        TextView textViewEndDate = (TextView) rowView.findViewById(R.id.textViewEndDate);

        Calendar calendarEndDate = Calendar.getInstance();

        calendarEndDate.add(DAY_OF_MONTH, 30);

        textViewEndDate.setText(SIMPLE_DATE_FORMAT.format(calendarEndDate.getTime()));

        textViewStartDate.performClick();

        DatePickerDialog dateDialog = (DatePickerDialog) ShadowDatePickerDialog.getLatestDialog();

        calendarEndDate.add(DAY_OF_MONTH, -MIN_ORDER_PERIOD);

        Date maxDate = new Date(dateDialog.getDatePicker().getMaxDate());

        String maxStartDateAsText = SIMPLE_DATE_FORMAT.format(maxDate);

        String endDateAsText = SIMPLE_DATE_FORMAT.format(calendarEndDate.getTime());

        assertThat(maxStartDateAsText, is(endDateAsText));

    }


    @Test
    public void shouldPutOrderReasonsIntoOrderReasonsSpinnerAdapter() {
        Spinner spinner = (Spinner) getViewFromListRow(adapter, list_item_layout, R.id.spinnerOrderReasons);
        String reasonName = ((OrderReason) spinner.getAdapter().getItem(0)).getReason();
        assertThat(reasonName, is(emergency.getReason()));
    }

    @Test
    public void shouldShowUnExpectedReasonsSpinnerIfDataIsUnexpected() throws Exception {
        ArrayList<OrderCommodityViewModel> commodities = new ArrayList<>();
        commodities.add(new OrderCommodityViewModel(new Commodity("item")));
        adapter = new SelectedOrderCommoditiesAdapter(Robolectric.application, list_item_layout, commodities, orderReasons);
        ViewGroup genericLayout = new LinearLayout(Robolectric.application);

        View convertView = LayoutInflater.from(Robolectric.application).inflate(list_item_layout, null);

        View rowView = adapter.getView(0, convertView, genericLayout);

        EditText editTextOrderQuantity = (EditText) rowView.findViewById(R.id.editTextOrderQuantity);

        Spinner spinnerUnexpectedQuantityReasons = (Spinner) rowView.findViewById(R.id.spinnerUnexpectedQuantityReasons);

        assertThat(spinnerUnexpectedQuantityReasons, is(notNullValue()));

        editTextOrderQuantity.setText("20");

        ShadowHandler.idleMainLooper();

        MatcherAssert.assertThat(ShadowToast.getTextOfLatestToast(), equalTo("Unexpected quantity (12) entered  for item. Please give a reason."));

        assertThat(spinnerUnexpectedQuantityReasons.getVisibility(), is(View.VISIBLE));

        editTextOrderQuantity.setText("2");

        assertThat(spinnerUnexpectedQuantityReasons.getVisibility(), is(View.INVISIBLE));
    }

    @Test
    public void shouldSetEndDateGivenStartDateAndTheOrderReasonIsRoutine() throws Exception {
        orderReasons = new ArrayList<>();
        orderReasons.add(routine);
        orderReasons.add(emergency);

        adapter = new SelectedOrderCommoditiesAdapter(Robolectric.application, list_item_layout, commodities, orderReasons);

        ViewGroup genericLayout = new LinearLayout(Robolectric.application);

        View convertView = LayoutInflater.from(Robolectric.application).inflate(list_item_layout, null);

        View rowView = adapter.getView(0, convertView, genericLayout);

        TextView startDate = (TextView) rowView.findViewById(R.id.textViewStartDate);

        startDate.setText("01-Jan-14");

        Spinner spinner = (Spinner) rowView.findViewById(R.id.spinnerOrderReasons);

        spinner.setSelection(0);

        TextView textViewEndDate = (TextView) rowView.findViewById(R.id.textViewEndDate);

        String endDate = textViewEndDate.getText().toString();

        assertThat(endDate, is("31-Jan-14"));
    }

    @Test
    public void shouldDisableEndDateWhenOrderReasonIsRoutine() throws Exception {
        orderReasons = new ArrayList<>();
        orderReasons.add(routine);
        orderReasons.add(emergency);

        adapter = new SelectedOrderCommoditiesAdapter(Robolectric.application, list_item_layout, commodities, orderReasons);

        ViewGroup genericLayout = new LinearLayout(Robolectric.application);

        View convertView = LayoutInflater.from(Robolectric.application).inflate(list_item_layout, null);

        View rowView = adapter.getView(0, convertView, genericLayout);

        Spinner spinnerOrderReasons = (Spinner) rowView.findViewById(R.id.spinnerOrderReasons);

        spinnerOrderReasons.setSelection(0);

        TextView textViewEndDate = (TextView) rowView.findViewById(R.id.textViewEndDate);

        assertThat(textViewEndDate.isEnabled(), is(false));
    }

    @Test
    public void shouldShowRoutineAsTheDefaultReasonForOrder() throws Exception {
        when(commodityViewModel.getOrderReasonPosition()).thenReturn(null);
        adapter =  new SelectedOrderCommoditiesAdapter(Robolectric.application, list_item_layout, commodities, orderReasons);
        ViewGroup genericLayout = new LinearLayout(Robolectric.application);
        View convertView = LayoutInflater.from(Robolectric.application).inflate(list_item_layout, null);
        View rowView = adapter.getView(0, convertView, genericLayout);

        Spinner spinnerOrderReasons = (Spinner) rowView.findViewById(R.id.spinnerOrderReasons);

        assertThat(((OrderReason) spinnerOrderReasons.getSelectedItem()).getReason(), is(ROUTINE));
    }

    @Ignore("WIP")
    @Test
    public void shouldShowSpinnerForUnexpectedOrderReasonsIfOrderDatesAreChangedWhenOrderReasonIsRoutine() throws Exception {
        when(commodityViewModel.getOrderReasonPosition()).thenReturn(null);
        adapter =  new SelectedOrderCommoditiesAdapter(Robolectric.application, list_item_layout, commodities, orderReasons);
        ViewGroup genericLayout = new LinearLayout(Robolectric.application);
        View convertView = LayoutInflater.from(Robolectric.application).inflate(list_item_layout, null);
        View rowView = adapter.getView(0, convertView, genericLayout);

        Spinner spinnerOrderReasons = (Spinner) rowView.findViewById(R.id.spinnerOrderReasons);

        assertThat(((OrderReason) spinnerOrderReasons.getSelectedItem()).getReason(), is(ROUTINE));
    }

    @Ignore("WIP")
    @Test
    public void shouldShowSpinnerForUnExpectedOrderReasonsIfOrderIsNotRoutine() throws Exception {

        assert (false);
    }

    @Ignore("WIP")
    @Test
    public void shouldDefaultToBlankForUnExpectedReasons() throws Exception {
        assert (false);

    }
}