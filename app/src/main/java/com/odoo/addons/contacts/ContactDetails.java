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
 * Created on 14/6/16 6:31 PM
 */
package com.odoo.addons.contacts;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
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
import com.odoo.base.addons.res.ResCountry;
import com.odoo.base.addons.res.ResPartner;
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
import com.odoo.core.utils.OStorageUtils;
import com.odoo.core.utils.OStringColorUtil;

import org.json.JSONArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import odoo.controls.OField;
import odoo.controls.OForm;

public class ContactDetails extends OdooCompatActivity implements
        OField.IOnFieldValueChangeListener,
        View.OnClickListener {

    // UI component
    @BindView(R.id.contact_collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.contact_image)
    ImageView contactImage;
    @BindView(R.id.captureImage)
    FloatingActionButton mCaptureImage;

    // Odoo component
    @BindView(R.id.name)
    OField nameField;
    @BindView(R.id.contactForm)
    OForm mForm;
    private ODataRow record;
    private OFileManager mOFileManager;

    // DB Component
    private ResPartner resPartner;
    private ResCountry resCountry;

    // Android Component
    private Menu mMenu;
    private Bundle extras;

    // Java Component
    private Boolean mEditMode = false;
    private String mNewImage = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_detail);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mOFileManager = new OFileManager(this);
        resPartner = new ResPartner(this, null);
        resCountry = new ResCountry(this, null);
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
            ODataRow record = resPartner.browse(rowId);
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
            mMenu.findItem(R.id.menu_contact_detail_more).setVisible(!editMode);
            mMenu.findItem(R.id.menu_contact_edit).setVisible(!editMode);
            mMenu.findItem(R.id.menu_contact_save).setVisible(editMode);
            mMenu.findItem(R.id.menu_contact_cancel).setVisible(editMode);
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
            contactImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            contactImage.setImageBitmap(getImage());
        } else {
            contactImage.setColorFilter(Color.parseColor("#ffffff"));
        }
    }

    private Bitmap getImage(){
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
        return img;
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
        getMenuInflater().inflate(R.menu.menu_contact_detail, menu);
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
            case R.id.menu_contact_edit:
                mEditMode = !mEditMode;
                setMode(mEditMode);
                break;
            case R.id.menu_contact_save:
                final OValues values = mForm.getValues();
                if (values != null){
                    if (inNetwork()) {
                        ODataRow country = resCountry.browse(values.getInt("country_id"));
                        values.put("country_id", country.getInt("id"));
                        new ContactSaveOperation().execute(values);
                    } else {
                        Toast.makeText(this, R.string.toast_network_required, Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case R.id.menu_contact_cancel:
                mEditMode = !mEditMode;
                if (record != null){
                    setMode(mEditMode);
                } else {
                    finish();
                }
                break;
            case R.id.menu_contact_share:
                new ContactShareOperation().execute();
                break;
            case R.id.menu_contact_delete:
                if (inNetwork()){
                    OAlert.showConfirm(this, OResource.string(this,
                            R.string.label_confirm_delete),
                            new OAlert.OnAlertConfirmListener() {
                                @Override
                                public void onConfirmChoiceSelect(OAlert.ConfirmType type) {
                                    if (type == OAlert.ConfirmType.POSITIVE) {
                                        new ContactArchiveOperation().execute();
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
        IntentUtils.startActivity(ContactDetails.this, ContactDetails.class, data);
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
            contactImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            contactImage.setColorFilter(null);
            contactImage.setImageBitmap(BitmapUtils.getBitmapImage(this, mNewImage));
        } else if (values != null) {
            Toast.makeText(this, R.string.toast_image_size_too_large, Toast.LENGTH_LONG).show();
        }
    }

    private class ContactSaveOperation extends AsyncTask<OValues, Void, Boolean> {

        private ODataRow results;
        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(ContactDetails.this);
            mDialog.setTitle(R.string.title_working);
            mDialog.setCancelable(false);
            mDialog.show();
        }

        @Override
        protected Boolean doInBackground(OValues... params) {
            boolean status = false;
            final OValues values = params[0];
            try {
                final ORecordValues data = ResPartner.valuesToData(values);
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
                    int newID = resPartner.getServerDataHelper().createOnServer(data);
                    values.put("id", newID);
                    results = resPartner.quickCreateRecord(values.toDataRow());
                } else {
                    resPartner.getServerDataHelper().updateOnServer(data, record.getInt("id"));
                    results = resPartner.quickCreateRecord(record);
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
                Toast.makeText(ContactDetails.this, msg, Toast.LENGTH_LONG).show();
                reloadActivity(results);
            } else {
                Toast.makeText(ContactDetails.this, R.string.error_general, Toast.LENGTH_LONG).show();
            }
        }

    }


    private class ContactArchiveOperation extends AsyncTask<Void, Void, Boolean> {

        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(ContactDetails.this);
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
                resPartner.getServerDataHelper().callMethod("toggle_active", args);
                ODomain domain = new ODomain();
                domain.add("id", "=", record.getInt("id"));
                resPartner.quickSyncRecords(domain);
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
                Toast.makeText(ContactDetails.this, R.string.error_general, Toast.LENGTH_LONG).show();
            }
        }

    }


    private class ContactShareOperation extends AsyncTask<Void, Void, Boolean> {

        private File imgFile;
        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(ContactDetails.this);
            mDialog.setTitle(R.string.title_working);
            mDialog.setCancelable(false);
            mDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean status ;
            try {
                OutputStream imgOut = null;
                imgFile = new File(OStorageUtils.getDirectoryPath("image"), record.getString("name") + ".jpg");
                imgOut = new FileOutputStream(imgFile);
                Bitmap bmp = getImage();
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, imgOut);
                imgOut.flush();
                imgOut.close();
                status = true;
            } catch (FileNotFoundException e){
                status = false;
            } catch (IOException e){
                status = false;
            }

            return status;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            mDialog.dismiss();
            if (success) {
                Intent shareCaptionIntent = new Intent();
                shareCaptionIntent.setAction(Intent.ACTION_SEND);
                shareCaptionIntent.setType("image/*");
                shareCaptionIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imgFile));
                //set caption
                String text = "*"
                        + record.getString("name")
                        + "*"
                        + "\n"
                        + record.getString("currency_symbol")
                        + " "
                        + record.getString("lst_price");
                // shareCaptionIntent.putExtra(Intent.EXTRA_TITLE, record.getString("name"));
                // shareCaptionIntent.putExtra(Intent.EXTRA_SUBJECT, record.getString("name"));
                shareCaptionIntent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(shareCaptionIntent, "Share To"));
            } else {
                Toast.makeText(ContactDetails.this, R.string.error_general, Toast.LENGTH_LONG).show();
            }
        }
    }

}
