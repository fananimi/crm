package com.odoo.addons.product;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.R;
import com.odoo.base.addons.ir.feature.OFileManager;
import com.odoo.base.addons.product.ProductCategory;
import com.odoo.base.addons.product.ProductTemplate;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.support.OdooCompatActivity;
import com.odoo.core.utils.BitmapUtils;
import com.odoo.core.utils.IntentUtils;
import com.odoo.core.utils.OAlert;
import com.odoo.core.utils.OResource;
import com.odoo.core.utils.OStringColorUtil;

import org.json.JSONArray;

import butterknife.BindView;
import butterknife.ButterKnife;
import odoo.controls.OField;
import odoo.controls.OForm;

/**
 * Created by fanani on 12/26/17.
 */

public class ProductDetail extends OdooCompatActivity implements
        OField.IOnFieldValueChangeListener,
        View.OnClickListener {

    // UI component
    @BindView(R.id.product_collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.product_image)
    ImageView productImage;
    @BindView(R.id.captureImage)
    FloatingActionButton mCaptureImage;

    // Odoo component
    @BindView(R.id.name)
    OField nameField;
    @BindView(R.id.productForm)
    OForm mForm;
    private ODataRow record;
    private OFileManager mOFileManager;

    // DB Component
    private ProductTemplate productTemplate;
    private ProductCategory productCategory;

    // Android Component
    private Menu mMenu;
    private Bundle extras;
    private Boolean mEditMode = false;
    private String mNewImage = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_detail);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mOFileManager = new OFileManager(this);
        productTemplate = new ProductTemplate(this, null);
        productCategory = new ProductCategory(this, null);
        extras = getIntent().getExtras();
        record = getRecordInExtra();
        if (record == null){
            mEditMode = true;
        }
        setMode(mEditMode);
    }

    private ODataRow getRecordInExtra() {
        boolean hasRecord = extras != null && extras.containsKey(OColumn.ROW_ID);
        if (hasRecord){
            int rowId = extras.getInt(OColumn.ROW_ID);
            ODataRow record = productTemplate.browse(rowId);
            return record;
        }
        return null;
    }

    private void setMode(Boolean editMode) {
        // image manipulation
        setImage();
        mCaptureImage.setVisibility(editMode? View.VISIBLE : View.GONE);
        mCaptureImage.setOnClickListener(this);

        // menu manipulation
        if (mMenu != null) {
            mMenu.findItem(R.id.menu_product_detail_more).setVisible(!editMode);
            mMenu.findItem(R.id.menu_product_edit).setVisible(!editMode);
            mMenu.findItem(R.id.menu_product_save).setVisible(editMode);
            mMenu.findItem(R.id.menu_product_cancel).setVisible(editMode);
        }

        // form manipulation
        if (editMode) {
            nameField.setVisibility(View.VISIBLE);
            nameField.setOnValueChangeListener(this);
        } else {
            nameField.setVisibility(View.GONE);
        }
        mForm.setEditable(mEditMode);
        int color = 0;
        if (record != null){
            color = OStringColorUtil.getStringColor(this, record.getString("name"));
            collapsingToolbarLayout.setTitle(record.getString("name"));
        } else {
            color = Color.DKGRAY;
            collapsingToolbarLayout.setTitle("New");
        }
        mForm.setIconTintColor(color);
        mForm.initForm(record);
    }

    private void setImage() {
        if (record != null){
            Bitmap img;
            if (!record.getString("image").equals("false")) {
                img = BitmapUtils.getBitmapImage(this, record.getString("image"));
            } else if (!record.getString("image_medium").equals("false")) {
                img = BitmapUtils.getBitmapImage(this, record.getString("image_medium"));
            } else if (!record.getString("image_small").equals("false")){
                img = BitmapUtils.getBitmapImage(this, record.getString("image_small"));
            } else {
                img = BitmapUtils.getAlphabetImage(this, record.getString("name"));
            }
            productImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            productImage.setImageBitmap(img);
        } else {
            productImage.setColorFilter(Color.parseColor("#ffffff"));
        }
    }

    public boolean inNetwork() {
        App app = (App) getApplicationContext().getApplicationContext();
        return app.inNetwork();
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
        getMenuInflater().inflate(R.menu.menu_product_detail, menu);
        mMenu = menu;
        setMode(mEditMode);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                back();
                break;
            case R.id.menu_product_edit:
                mEditMode = !mEditMode;
                setMode(mEditMode);
                break;
            case R.id.menu_product_save:
                final OValues values = mForm.getValues();
                if (values != null){
                    if (inNetwork()) {
                        ODataRow category = productCategory.browse(values.getInt("categ_id"));
                        values.put("categ_id", category.getInt("id"));
                        new ProductSaveOperation().execute(values);
                    } else {
                        Toast.makeText(this, R.string.toast_network_required, Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case R.id.menu_product_cancel:
                mEditMode = !mEditMode;
                if (record != null){
                    setMode(mEditMode);
                } else {
                    finish();
                }
                break;
            case R.id.menu_product_share:
                break;
            case R.id.menu_product_delete:
                if (inNetwork()){
                    OAlert.showConfirm(this, OResource.string(this,
                            R.string.label_confirm_delete),
                            new OAlert.OnAlertConfirmListener() {
                                @Override
                                public void onConfirmChoiceSelect(OAlert.ConfirmType type) {
                                    if (type == OAlert.ConfirmType.POSITIVE) {
                                        new ProductArchiveOperation().execute();
                                    }
                                }
                            });
                } else {
                    Toast.makeText(this, R.string.toast_network_required, Toast.LENGTH_LONG).show();
                }
                break;
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
        if (field.getFieldName().equals("name")) {
            String title = value.toString();
            if (!TextUtils.isEmpty(title)){
                collapsingToolbarLayout.setTitle(value.toString());
            }
        }
    }

    private void reloadActivity(ODataRow record) {
        Bundle data = record.getPrimaryBundleData();
        IntentUtils.startActivity(ProductDetail.this, ProductDetail.class, data);
        finish();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.captureImage:
                mOFileManager.requestForFile(OFileManager.RequestType.IMAGE_OR_CAPTURE_IMAGE);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        OValues values = mOFileManager.handleResult(requestCode, resultCode, data);
        if (values != null && !values.contains("size_limit_exceed")) {
            mNewImage = values.getString("datas");
            productImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            productImage.setColorFilter(null);
            productImage.setImageBitmap(BitmapUtils.getBitmapImage(this, mNewImage));
        } else if (values != null) {
            Toast.makeText(this, R.string.toast_image_size_too_large, Toast.LENGTH_LONG).show();
        }
    }

    private class ProductSaveOperation extends AsyncTask<OValues, Void, Boolean> {

        private ODataRow results;
        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(ProductDetail.this);
            mDialog.setTitle(R.string.title_working);
            mDialog.setCancelable(false);
            mDialog.show();
        }

        @Override
        protected Boolean doInBackground(OValues... params) {
            boolean status = false;
            final OValues values = params[0];
            try {
                final ORecordValues data = ProductTemplate.valuesToData(values);
                if (mNewImage != null){
                    data.put("image", mNewImage);
                }
                if (record == null){
                    // create new record
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDialog.setMessage("Creating " + values.getString("name"));
                        }
                    });
                    Thread.sleep(500);
                    int newID = productTemplate.getServerDataHelper().createOnServer(data);
                    values.put("id", newID);
                    results = productTemplate.quickCreateRecord(values.toDataRow());
                } else {
                    productTemplate.getServerDataHelper().updateOnServer(data, record.getInt("id"));
                    results = productTemplate.quickCreateRecord(record);
                }
                status = true;
            } catch (Exception ex){
                ex.printStackTrace();
                status = false;
            }
            return status;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            mDialog.dismiss();
            if (success) {
                final String product_name = results.getString("name");
                final String msg = (record != null) ? product_name + " updated" :  product_name + " created";
                Toast.makeText(ProductDetail.this, msg, Toast.LENGTH_LONG).show();
                reloadActivity(results);
            } else {
                Toast.makeText(ProductDetail.this, R.string.error_general, Toast.LENGTH_LONG).show();
            }
        }

    }


    private class ProductArchiveOperation extends AsyncTask<Void, Void, Boolean> {

        private ODataRow results;
        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(ProductDetail.this);
            mDialog.setTitle(R.string.title_working);
            mDialog.setCancelable(false);
            mDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean status ;
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDialog.setMessage("Archiving " + record.getString("name"));
                    }
                });
                Thread.sleep(500);
                OArguments args = new OArguments();
                args.add(new JSONArray().put(record.getInt("id")));
                productTemplate.getServerDataHelper().callMethod("toggle_active", args);
                ODomain domain = new ODomain();
                domain.add("id", "=", record.getInt("id"));
                productTemplate.quickSyncRecords(domain);
                status = true;
            } catch (Exception ex){
                ex.printStackTrace();
                status = false;
            }
            return status;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            mDialog.dismiss();
            if (success) {
                finish();
            } else {
                Toast.makeText(ProductDetail.this, R.string.error_general, Toast.LENGTH_LONG).show();
            }
        }

    }

}
