/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * <p/>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * <p/>
 * Created on 13/1/15 5:09 PM
 */
package com.odoo.addons.sale;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.base.addons.product.ProductProduct;
import com.odoo.base.addons.product.ProductTemplate;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.base.addons.sale.SaleOrder;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.support.OdooCompatActivity;
import com.odoo.core.utils.IntentUtils;
import com.odoo.core.utils.OAlert;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import odoo.controls.ExpandableListControl;
import odoo.controls.OField;
import odoo.controls.OForm;

/**
 * Created by fanani on 12/26/17.
 */

public class SaleDetail extends OdooCompatActivity implements
        OField.IOnFieldValueChangeListener,
        View.OnClickListener {

    public static final String TAG = SaleDetail.class.getSimpleName();
    public static final String EXTRA_KEY_EDIT_MODE = "extra_key_edit_mode";

    // UI component
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.addItem)
    LinearLayout mAddItem;
    @BindView(R.id.untaxedTotal)
    TextView untaxedTotal;
    @BindView(R.id.taxesTotal)
    TextView taxesTotal;
    @BindView(R.id.total)
    TextView total;
    @BindView(R.id.currency1)
    TextView currency1;
    @BindView(R.id.currency2)
    TextView currency2;
    @BindView(R.id.currency3)
    TextView currency3;
    @BindView(R.id.expListOrderLine)
    ExpandableListControl mList;

    // Odoo component
    @BindView(R.id.saleForm)
    OForm mForm;
    @BindView(R.id.partner_id)
    OField mPartnerId;
    private ODataRow record;

    // DB Component
    private SaleOrder saleOrder;
    private ResPartner resPartner;
    private ProductProduct productProduct;

    // Android Component
    private Menu mMenu;
    private Bundle extras;
    private ExpandableListControl.ExpandableListAdapter mAdapter;

    // Java Component
    private Boolean mEditMode = false;
    private Sales.Type mType;
    private List<Object> orderLines = new ArrayList<>();
    private HashMap<String, Float> mLineQuantities = new HashMap<>();
    private HashMap<String, Integer> mLineIds = new HashMap<>();

    // Punya Default //
//    public static final int REQUEST_ADD_ITEMS = 323;
//    private ActionBar actionBar;
//    private ExpandableListControl mList;
//    private TextView txvType, currency1, currency2, currency3, untaxedAmt, taxesAmt, total_amt;
//    private ODataRow currencyObj;
//    private ResPartner partner = null;
//    private ProductProduct products = null;
//    private String mSOType = "";
//    private LinearLayout layoutAddItem = null;
//    private Type mType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sale_detail);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        saleOrder = new SaleOrder(this, null);
        resPartner = new ResPartner(this, null);
        productProduct = new ProductProduct(this, null);
        extras = getIntent().getExtras();
        mType = Sales.Type.valueOf(extras.getString(Sales.EXTRA_KEY_TYPE));
        record = getRecordInExtra();
        if (record == null){
            mEditMode = true;
        } else {
            mEditMode = extras.getBoolean(EXTRA_KEY_EDIT_MODE, false);
        }
        setMode();
    }

    private ODataRow getRecordInExtra() {
        boolean hasRecord = extras != null && extras.containsKey(OColumn.ROW_ID);
        if (hasRecord){
            int rowId = extras.getInt(OColumn.ROW_ID);
            ODataRow record = saleOrder.browse(rowId);
            return record;
        }
        return null;
    }

    private void setMode() {
        String currency_symbol = saleOrder.getDefaultCurrencySymbol();
        String title = (record == null) ? "New Quotation" : record.getString("name");
        toolbar.setTitle(title);

        // menu manipulation
        int stateIcon = R.drawable.ic_action_quotation;
        switch (mType){
            case Quotation:
                stateIcon = R.drawable.ic_action_quotation;
                modeQuotation();
                break;
            case SaleOrder:
                stateIcon = R.drawable.ic_action_sale_order;
                modeSaleOrder();
                break;
        }

        // form manipulation
        mForm.setEditable(mEditMode);
        mForm.initForm(record);
        if (record == null){
            mPartnerId.setOnValueChangeListener(this);
            // amount calculation
            untaxedTotal.setText("0.00");
            taxesTotal.setText("0.00");
            total.setText("0.00");
        } else {
            mPartnerId.setEditable(false);
            // amount calculation
            untaxedTotal.setText(String.format("%.2f", record.getFloat("amount_untaxed")));
            taxesTotal.setText(String.format("%.2f", record.getFloat("amount_tax")));
            total.setText(String.format("%.2f", record.getFloat("amount_total")));
            currency_symbol = record.getM2ORecord("currency_id").browse().getString("symbol");
        }
        // print currency symbol
        currency1.setText(currency_symbol);
        currency2.setText(currency_symbol);
        currency3.setText(currency_symbol);

        // show / unshow addItem button
        mAddItem.setOnClickListener(this);
        if (mEditMode){
            mAddItem.setVisibility(View.VISIBLE);
        } else {
            mAddItem.setVisibility(View.GONE);
        }
        initAdapter();
    }

    private void modeQuotation(){
        if (mMenu != null) {
            boolean editMode = (record == null) ? false : mEditMode;
            mMenu.findItem(R.id.menu_sale_detail_more).setVisible(!mEditMode);
            mMenu.findItem(R.id.menu_sale_edit).setVisible(!mEditMode);
            mMenu.findItem(R.id.menu_sale_save).setVisible(editMode);
            mMenu.findItem(R.id.menu_sale_cancel).setVisible(editMode);
        }
    }

    private void modeSaleOrder(){
        if (mMenu != null) {
            mMenu.findItem(R.id.menu_sale_detail_more).setVisible(!mEditMode);
            // CRUD Operation always false while in Sale Order
            mMenu.findItem(R.id.menu_sale_edit).setVisible(false);
            mMenu.findItem(R.id.menu_sale_save).setVisible(false);
            mMenu.findItem(R.id.menu_sale_cancel).setVisible(false);
        }
    }

    private void initAdapter() {
        orderLines.clear();
        mList.setVisibility(View.VISIBLE);
        if (record != null){
            List<ODataRow> lines = record.getO2MRecord("order_line").browseEach();
            for (ODataRow line : lines) {
                int product_id = productProduct.selectServerId(line.getInt("product_id"));
                mLineQuantities.put(product_id + "", line.getFloat("product_uom_qty"));
                mLineIds.put(product_id + "", line.getInt("id"));
            }
            orderLines.addAll(lines);
        }
        mAdapter = mList.getAdapter(R.layout.sale_order_line_item, orderLines,
                new ExpandableListControl.ExpandableListAdapterGetViewListener() {
                    @Override
                    public View getView(int position, View mView, ViewGroup parent) {
                        ODataRow row = (ODataRow) mAdapter.getItem(position);
                        OControls.setText(mView, R.id.edtName, row.getString("name"));
                        OControls.setText(mView, R.id.edtProductQty, row.getString("product_uom_qty"));
                        OControls.setText(mView, R.id.edtProductPrice, String.format("%.2f", row.getFloat("price_unit")));
                        OControls.setText(mView, R.id.edtSubTotal, String.format("%.2f", row.getFloat("price_subtotal")));
                        return mView;
                    }
                });
        mAdapter.notifyDataSetChanged(orderLines);
    }

    private void back(){
        if (mEditMode){
            OAlert.showConfirm(this, OResource.string(this,
                    R.string.label_confirm_discard),
                    new OAlert.OnAlertConfirmListener() {
                        @Override
                        public void onConfirmChoiceSelect(OAlert.ConfirmType type) {
                            if (type == OAlert.ConfirmType.POSITIVE) {
                                finish();
                            }
                        }
                    });
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sale_detail, menu);
        mMenu = menu;
        setMode();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        OValues values = mForm.getValues();
//        App app = (App) getApplicationContext();
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                back();
                break;
            case R.id.menu_sale_edit:
                mEditMode = !mEditMode;
                setMode();
                break;

            case R.id.menu_sale_save:
                final OValues values = mForm.getValues();
                if (values != null){
//                    if (inNetwork()) {
//                        if (mNewImage != null) {
//                            values.put("image", mNewImage);
//                        }
//                        if (!TextUtils.equals(values.getString("country_id"), "false")){
//                            ODataRow country = resCountry.browse(values.getInt("country_id"));
//                            values.put("country_id", country.getInt("id"));
//                        }
//                        if (!TextUtils.equals(values.getString("state_id"), "false")){
//                            ODataRow state = resCountryState.browse(values.getInt("state_id"));
//                            values.put("state_id", state.get("id"));
//                        }
//                        new ContactSaveOperation().execute(values);
//                    } else {
//                        Toast.makeText(this, R.string.toast_network_required, Toast.LENGTH_LONG).show();
//                    }
                }
                break;
//            case R.id.menu_sale_save:
//                if (values != null) {
//                    if (app.inNetwork()) {
//                        values.put("partner_name", partner.getName(values.getInt("partner_id")));
//                        SaleOrderOperation saleOrderOperation = new SaleOrderOperation();
//                        saleOrderOperation.execute(values);
//                    } else {
//                        Toast.makeText(this, R.string.toast_network_required, Toast.LENGTH_LONG).show();
//                    }
//                }
//                break;
            case R.id.menu_sale_cancel:
                mEditMode = !mEditMode;
                if (record != null){
                    setMode();
                } else {
                    finish();
                }
                break;
//            case R.id.menu_sale_confirm_sale:
//                if (record != null) {
//                    if (extra != null && record.getFloat("amount_total") > 0) {
//                        if (app.inNetwork()) {
//                            sale.confirmSale(record, confirmSale);
//                        } else {
//                            Toast.makeText(this, R.string.toast_network_required, Toast.LENGTH_LONG).show();
//                        }
//                    } else {
//                        OAlert.showWarning(this, R.string.label_no_order_line + "");
//                    }
//                }
//                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        back();
    }

    @Override
    public void onFieldValueChange(OField field, Object value) {
        if (field.getFieldName().equals("partner_id")) {
            final OValues values = mForm.getValues();
            if (record == null && value != null){
                ODataRow partner = resPartner.browse(values.getInt("partner_id"));
                values.put("partner_id", partner.getInt("id"));
                new QuotationSaveOperation().execute(values);
            }
        }
    }

    private void reloadActivity(ODataRow record) {
        Bundle data = record.getPrimaryBundleData();
        data.putString(Sales.EXTRA_KEY_TYPE, extras.getString(Sales.EXTRA_KEY_TYPE));
        data.putBoolean(EXTRA_KEY_EDIT_MODE, true);
        IntentUtils.startActivity(SaleDetail.this, SaleDetail.class, data);
        finish();
    }

//    private class SaleOrderOperation extends AsyncTask<OValues, Void, Boolean> {
//
//        private ProgressDialog mDialog;
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            mDialog = new ProgressDialog(SalesDetail.this);
//            mDialog.setTitle(R.string.title_working);
//            mDialog.setMessage("Creating lines");
//            mDialog.setCancelable(false);
//            mDialog.show();
//        }
//
//        @Override
//        protected Boolean doInBackground(OValues... params) {
//            try {
//                Thread.sleep(500);
//                OValues values = params[0];
//                // Creating oneToMany order lines
//                JSONArray order_line = new JSONArray();
//                for (Object line : objects) {
//                    JSONArray o_line = new JSONArray();
//                    ODataRow row = (ODataRow) line;
//                    String product_id = row.getString("product_id");
//                    o_line.put((lineIds.containsKey(product_id)) ? 1 : 0);
//                    o_line.put((lineIds.containsKey(product_id)) ? lineIds.get(product_id) : false);
//                    if (lineIds.containsKey(product_id)) {
//                        JSONObject line_data = new JSONObject();
//                        line_data.put("product_uom_qty", row.get("product_uom_qty"));
//                        line_data.put("product_uos_qty", row.get("product_uos_qty"));
//                        o_line.put(line_data);
//                    } else
//                        o_line.put(JSONUtils.toJSONObject(row));
//                    order_line.put(o_line);
//                    lineIds.remove(product_id);
//                }
//                if (lineIds.size() > 0) {
//                    for (String key : lineIds.keySet()) {
//                        JSONArray o_line = new JSONArray();
//                        o_line.put(2);
//                        o_line.put(lineIds.get(key));
//                        o_line.put(false);
//                        order_line.put(o_line);
//                    }
//                }
//                Thread.sleep(500);
//                ORecordValues data = new ORecordValues();
//                data.put("name", values.getString("name"));
//                data.put("partner_id", partner.selectServerId(values.getInt("partner_id")));
//                data.put("date_order", values.getString("date_order"));
//                data.put("payment_term", values.get("payment_term"));
//                data.put("order_line", order_line);
//
//                if (record == null) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mDialog.setMessage("Creating " + mSOType);
//                        }
//                    });
//                    Thread.sleep(500);
//                    int new_id = sale.getServerDataHelper().createOnServer(data);
//                    values.put("id", new_id);
//                    ODataRow record = new ODataRow();
//                    record.put("id", new_id);
//                    sale.quickCreateRecord(record);
//                    //sale.insert(values);
//                } else {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mDialog.setMessage("Updating " + mSOType);
//                        }
//                    });
//                    Thread.sleep(500);
//                    sale.getServerDataHelper().updateOnServer(data, record.getInt("id"));
//                    sale.quickCreateRecord(record);
//                }
//                return true;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return false;
//        }
//
//        @Override
//        protected void onPostExecute(Boolean success) {
//            super.onPostExecute(success);
//            mDialog.dismiss();
//            if (success) {
//                Toast.makeText(SalesDetail.this, (record != null) ? mSOType + " updated"
//                        : mSOType + " created", Toast.LENGTH_LONG).show();
//                finish();
//            }
//        }
//    }

//    SaleOrder.OnOperationSuccessListener cancelOrder = new SaleOrder.OnOperationSuccessListener() {
//        @Override
//        public void OnSuccess() {
//            Toast.makeText(SalesDetail.this, StringUtils.capitalizeString(extra.getString("type"))
//                    + " cancelled", Toast.LENGTH_LONG).show();
//            finish();
//        }
//
//        @Override
//        public void OnCancelled() {
//
//        }
//    };

//    SaleOrder.OnOperationSuccessListener confirmSale = new SaleOrder.OnOperationSuccessListener() {
//        @Override
//        public void OnSuccess() {
//            Toast.makeText(SalesDetail.this, R.string.label_quotation_confirm, Toast.LENGTH_LONG).show();
//            finish();
//        }
//
//        @Override
//        public void OnCancelled() {
//
//        }
//    };

    @Override
    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.layoutAddItem:
//                if (mForm.getValues() != null) {
//                    Intent intent = new Intent(this, AddProductLineWizard.class);
//                    Bundle extra = new Bundle();
//                    for (String key : lineValues.keySet()) {
//                        extra.putFloat(key, lineValues.get(key));
//                    }
//                    intent.putExtras(extra);
//                    startActivityForResult(intent, REQUEST_ADD_ITEMS);
//                }
//                break;
//        }
    }

    private class QuotationSaveOperation extends AsyncTask<OValues, Void, Boolean> {

        private ODataRow results;
        private ProgressDialog mDialog;
        private String mMessage;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(SaleDetail.this);
            mDialog.setTitle(R.string.title_working);
            mDialog.setCancelable(false);
            mDialog.show();
        }

        @Override
        protected Boolean doInBackground(OValues... params) {
            boolean status = false;
            final OValues values = params[0];
            try {
                final ORecordValues data = SaleOrder.valuesToData(values);
                // create new record
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDialog.setMessage("Preparing new quotation");
                    }
                });
                Thread.sleep(500);
                int id = saleOrder.getServerDataHelper().createOnServer(data);
                values.put("id", id);
                results = saleOrder.quickCreateRecord(values.toDataRow());
                status = true;
            } catch (Exception ex){
                ex.printStackTrace();
                mMessage = ex.toString();
                status = false;
            }
            return status;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            mDialog.dismiss();
            if (success) {
                final String reference = results.getString("name");
                final String msg = reference + " Created";
                Toast.makeText(SaleDetail.this, msg, Toast.LENGTH_LONG).show();
                reloadActivity(results);
            } else {
                mPartnerId.setValue("false");
                Toast.makeText(SaleDetail.this, mMessage, Toast.LENGTH_LONG).show();
            }
        }

    }

//    private class OnCustomerChangeUpdate extends AsyncTask<ODataRow, Void, Void> {
//        private ProgressDialog progressDialog;
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            progressDialog = new ProgressDialog(SalesDetail.this);
//            progressDialog.setCancelable(false);
//            progressDialog.setTitle(R.string.title_please_wait);
//            progressDialog.setMessage(OResource.string(SalesDetail.this, R.string.title_working));
//            progressDialog.show();
//        }
//
//        @Override
//        protected Void doInBackground(ODataRow... params) {
//            sale.onPartnerIdChange(params[0]);
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Void aVoid) {
//            super.onPostExecute(aVoid);
//            progressDialog.dismiss();
//        }
//    }

//    private class OnProductChange extends AsyncTask<HashMap<String, Float>, Void, List<ODataRow>> {
//        private ProgressDialog progressDialog;
//        private String warning = null;
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            progressDialog = new ProgressDialog(SalesDetail.this);
//            progressDialog.setCancelable(false);
//            progressDialog.setTitle(R.string.title_please_wait);
//            progressDialog.setMessage(OResource.string(SalesDetail.this, R.string.title_working));
//            progressDialog.show();
//        }
//
//        @Override
//        protected List<ODataRow> doInBackground(HashMap<String, Float>... params) {
//            final OValues[] formValues = new OValues[1];
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    formValues[0] = mForm.getValues();
//                }
//            });
//            List<ODataRow> items = new ArrayList<>();
//            try {
//                ProductProduct productProduct = new ProductProduct(SalesDetail.this, sale.getUser());
//                SalesOrderLine saleLine = new SalesOrderLine(SalesDetail.this, sale.getUser());
//                ResPartner partner = new ResPartner(SalesDetail.this, sale.getUser());
//                ODataRow customer = partner.browse(formValues[0].getInt("partner_id"));
//                ServerDataHelper helper = saleLine.getServerDataHelper();
//                boolean stockInstalled = saleLine.isInstalledOnServer("stock");
//                for (String key : params[0].keySet()) {
//                    ODataRow product = productProduct.browse(productProduct.selectRowId(Integer.parseInt(key)));
//                    Float qty = params[0].get(key);
//                    OArguments arguments = new OArguments();
//                    arguments.add(new JSONArray());
//                    int pricelist = customer.getInt("pricelist_id");
//                    arguments.add(pricelist); // Price List for customer
//                    arguments.add(product.getInt("id")); // product id
//                    arguments.add(qty); // Quantity
//                    arguments.add(false); // UOM
//                    arguments.add(qty); // Qty_UOS
//                    arguments.add(false);// UOS
//                    arguments.add((product.getString("name").equals("false")) ? false
//                            : product.getString("name"));
//                    arguments.add(customer.getInt("id")); // Partner id
//                    arguments.add(false); // lang
//                    arguments.add(true); // update_tax
//                    arguments.add((customer.getString("date_order").equals("false")) ? false
//                            : customer.getString("date_order")); // date order
//                    arguments.add(false); // packaging
//                    Object fiscal_position = (customer.getString("fiscal_position").equals("false"))
//                            ? false : customer.getString("fiscal_position");
//                    arguments.add(fiscal_position);// fiscal position
//                    arguments.add(false); // flag
//                    int version = saleLine.getOdooVersion().getVersionNumber();
//                    if (stockInstalled && version > 7) {
//                        arguments.add(false);
//                    }
//                    HashMap<String, Object> context = new HashMap<>();
//                    context.put("partner_id", customer.getInt("id"));
//                    context.put("quantity", qty);
//                    context.put("pricelist", pricelist);
//
//                    // Fixed for Odoo 7.0 no product_id_change_with_wh available for v7
//                    String method = (stockInstalled && version > 7) ? "product_id_change_with_wh" : "product_id_change";
//                    JSONObject response = ((JSONObject) helper.callMethod(method, arguments, context));
//                    JSONObject res = response.getJSONObject("value");
//                    if (response.has("warning") && !response.getString("warning").equals("false")) {
//                        JSONObject warning_data = response.getJSONObject("warning");
//                        if (warning_data.has("message"))
//                            warning = warning_data.getString("message");
//                    }
//                    OValues values = new OValues();
//                    values.put("product_id", product.getInt("id"));
//                    values.put("name", res.get("name"));
//                    values.put("product_uom_qty", res.get("product_uos_qty"));
//                    values.put("product_uom", res.get("product_uom"));
//                    values.put("price_unit", res.get("price_unit"));
//                    values.put("product_uos_qty", res.getDouble("product_uos_qty"));
//                    values.put("product_uos", false);
//                    values.put("price_subtotal", res.getDouble("price_unit") * res.getDouble("product_uos_qty"));
//                    JSONArray tax_id = new JSONArray();
//                    tax_id.put(6);
//                    tax_id.put(false);
//                    tax_id.put(res.getJSONArray("tax_id"));
//                    values.put("tax_id", new JSONArray().put(tax_id));
//                    values.put("th_weight", (res.has("th_weight")) ? res.get("th_weight") : 0);
//                    values.put("discount", (res.has("discount")) ? res.get("discount") : 0);
//                    if (stockInstalled) {
//                        values.put("route_id", (res.has("route_id")) ? res.get("route_id") : false);
//                        values.put("delay", res.get("delay"));
//                    }
//                    if (extra != null)
//                        values.put("order_id", extra.getInt(OColumn.ROW_ID));
//                    items.add(values.toDataRow());
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return items;
//        }
//
//        @Override
//        protected void onPostExecute(List<ODataRow> row) {
//            super.onPostExecute(row);
//            if (row != null) {
//                objects.clear();
//                objects.addAll(row);
//                mAdapter.notifyDataSetChanged(objects);
//                float total = 0.0f;
//                for (ODataRow rec : row) {
//                    total += rec.getFloat("price_subtotal");
//                }
//                total_amt.setText(String.format("%.2f", total));
//                untaxedAmt.setText(total_amt.getText());
//            }
//            progressDialog.dismiss();
//            if (warning != null) {
//                OAlert.showWarning(SalesDetail.this, warning.trim());
//            }
//        }
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_ADD_ITEMS && resultCode == Activity.RESULT_OK) {
//            lineValues.clear();
//            for (String key : data.getExtras().keySet()) {
//                if (data.getExtras().getFloat(key) > 0)
//                    lineValues.put(key, data.getExtras().getFloat(key));
//            }
//            OnProductChange onProductChange = new OnProductChange();
//            onProductChange.execute(lineValues);
//        }
//    }

}
