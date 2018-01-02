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
package com.odoo.addons.contact;

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
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.R;
import com.odoo.base.addons.ir.feature.OFileManager;
import com.odoo.base.addons.res.ResCountry;
import com.odoo.base.addons.res.ResCountryState;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

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
    private ResCountryState resCountryState;

    // Android Component
    private Menu mMenu;
    private Bundle extras;

    // Java Component
    private Boolean mEditMode = false;
    private String mNewImage = null;

    private enum ContactOperationType {
        Share,
        Import
    }

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
        resCountryState = new ResCountryState(this, null);
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
            findViewById(R.id.country_id).setVisibility(View.VISIBLE);
            findViewById(R.id.state_id).setVisibility(View.VISIBLE);
            findViewById(R.id.city).setVisibility(View.VISIBLE);
            findViewById(R.id.street).setVisibility(View.VISIBLE);
            findViewById(R.id.street2).setVisibility(View.VISIBLE);
            findViewById(R.id.full_address).setVisibility(View.GONE);
        } else {
            nameField.setVisibility(View.GONE);
            findViewById(R.id.country_id).setVisibility(View.GONE);
            findViewById(R.id.state_id).setVisibility(View.GONE);
            findViewById(R.id.city).setVisibility(View.GONE);
            findViewById(R.id.street).setVisibility(View.GONE);
            findViewById(R.id.street2).setVisibility(View.GONE);
            findViewById(R.id.full_address).setVisibility(View.VISIBLE);
            checkControls();
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

    private void checkControls() {
        findViewById(R.id.full_address).setOnClickListener(this);
        findViewById(R.id.website).setOnClickListener(this);
        findViewById(R.id.email).setOnClickListener(this);
        findViewById(R.id.phone_number).setOnClickListener(this);
        findViewById(R.id.mobile_number).setOnClickListener(this);
    }

    private void setImage() {
        if (record != null){
            contactImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap image = ResPartner.getImage(this, record);
            if (image != null){
                contactImage.setImageBitmap(image);
            }
        } else {
            contactImage.setColorFilter(Color.parseColor("#ffffff"));
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
                        if (mNewImage != null) {
                            values.put("image", mNewImage);
                        }
                        if (!TextUtils.equals(values.getString("country_id"), "false")){
                            ODataRow country = resCountry.browse(values.getInt("country_id"));
                            values.put("country_id", country.getInt("id"));
                        }
                        if (!TextUtils.equals(values.getString("state_id"), "false")){
                            ODataRow state = resCountryState.browse(values.getInt("state_id"));
                            values.put("state_id", state.get("id"));
                        }
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
                new ContactOpenOperation().execute(ContactOperationType.Share);
                break;
            case R.id.menu_contact_import:
                new ContactOpenOperation().execute(ContactOperationType.Import);
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
            case R.id.full_address:
                IntentUtils.redirectToMap(this, record.getString("full_address"));
                break;
            case R.id.website:
                IntentUtils.openURLInBrowser(this, record.getString("website"));
                break;
            case R.id.email:
                IntentUtils.requestMessage(this, record.getString("email"));
                break;
            case R.id.phone_number:
                IntentUtils.requestCall(this, record.getString("phone"));
                break;
            case R.id.mobile_number:
                IntentUtils.requestCall(this, record.getString("mobile"));
                break;
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
        private String mMessage;

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
                if (record == null) {
                    // create new record
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDialog.setMessage("Creating " + values.getString("name"));
                        }
                    });
                    Thread.sleep(500);
                    int id = resPartner.getServerDataHelper().createOnServer(data);
                    values.put("id", id);
                    results = resPartner.quickCreateRecord(values.toDataRow());
                } else {
                    // update record
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDialog.setMessage("Updating " + values.getString("name"));
                        }
                    });
                    Thread.sleep(500);
                    int id = resPartner.getServerDataHelper().updateOnServer(data, record.getInt("id"));
                    ODomain domain = new ODomain();
                    domain.add("id", "=", id);
                    resPartner.quickSyncRecords(domain);
                    results = record;
                }
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
                final String product_name = results.getString("name");
                final String msg = (record != null) ? product_name + " updated" :  product_name + " created";
                Toast.makeText(ContactDetails.this, msg, Toast.LENGTH_LONG).show();
                reloadActivity(results);
            } else {
                Toast.makeText(ContactDetails.this, mMessage, Toast.LENGTH_LONG).show();
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


    private class ContactOpenOperation extends AsyncTask<ContactOperationType, Void, Boolean> {

        private File contactFile;
        private ProgressDialog mDialog;
        private ContactOperationType mode;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(ContactDetails.this);
            mDialog.setTitle(R.string.title_working);
            mDialog.setCancelable(false);
            mDialog.show();
        }

        @Override
        protected Boolean doInBackground(ContactOperationType... params) {
            mode = params[0];
            boolean status ;
            try {
                // follow this instruction https://en.wikipedia.org/wiki/VCard
                contactFile = new File(OStorageUtils.getDirectoryPath("file"), record.getString("name") + ".vcf");
                FileWriter fw = new FileWriter(contactFile);
                fw.write("BEGIN:VCARD\r\n");
                fw.write("VERSION:3.0\n\n");
                fw.write("N:" + record.getString("name") + ";;;;\n\n");
                fw.write("FN:" + record.getString("name") + "\n\n");
                if (!TextUtils.equals(record.getString("email"), "false")){
                    fw.write("EMAIL:" + record.getString("email") + "\r\n");
                }
                if (!TextUtils.equals(record.getString("phone"), "false")){
                    fw.write("TEL;TYPE=HOME,VOICE:" + record.getString("phone") + "\r\n");
                }
                if (!TextUtils.equals(record.getString("mobile"), "false")){
                    fw.write("TEL;TYPE=HOME,VOICE:" + record.getString("mobile") + "\r\n");
                }
                String vAddr = "";
                vAddr += "ADR;TYPE=HOME:;;";
                if (!TextUtils.equals(record.getString("street"), "false")){
                    vAddr += record.getString("street");
                }
                vAddr += ";";
                if (!TextUtils.equals(record.getString("street2"), "false")){
                    vAddr += record.getString("street2");
                }
                vAddr += ";";
                if (!TextUtils.equals(record.getString("state_id"), "false")){
                    vAddr += record.getM2ORecord("state_id").browse().getString("name");
                }
                vAddr += ";";
                if (!TextUtils.equals(record.getString("zip"), "false")){
                    vAddr += record.getString("zip");
                }
                vAddr += ";";
                if (!TextUtils.equals(record.getString("country_id"), "false")){
                    vAddr += record.getM2ORecord("country_id").browse().getString("name");
                }
                vAddr += "\r\n";
                fw.write(vAddr);
                if (!TextUtils.equals(record.getString("website"), "false")){
                    fw.write("URL:" + record.getString("website") + "\r\n");
                }

                Bitmap image = ResPartner.getImage(ContactDetails.this, record);
                if (image != null){
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, 100, output);
                    byte[] byteArrayImage = output.toByteArray();
                    String base64Image = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
                    fw.write("PHOTO;TYPE=JPEG;ENCODING=b:" + base64Image);
                }

                fw.write("END:VCARD\n");
                fw.flush();
                fw.close();
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
                Intent shareContactIntent = new Intent();
                switch (mode){
                    case Import:
                        shareContactIntent.setAction(Intent.ACTION_VIEW);
                        shareContactIntent.setDataAndType(Uri.fromFile(contactFile), "text/x-vcard");
                        shareContactIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(shareContactIntent);
                        break;
                    case Share:
                        shareContactIntent.setAction(Intent.ACTION_SEND);
                        shareContactIntent.setType("text/x-vcard");
                        shareContactIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(contactFile));
                        startActivity(Intent.createChooser(shareContactIntent, "Share To"));
                        break;
                }
            } else {
                Toast.makeText(ContactDetails.this, R.string.error_general, Toast.LENGTH_LONG).show();
            }
        }
    }

}
