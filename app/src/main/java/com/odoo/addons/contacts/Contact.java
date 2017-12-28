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
 * Created on 30/12/14 3:28 PM
 */
package com.odoo.addons.contacts;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.support.addons.fragment.BaseFragment;
import com.odoo.core.support.addons.fragment.IOnSearchViewChangeListener;
import com.odoo.core.support.addons.fragment.ISyncStatusObserverListener;
import com.odoo.core.support.drawer.ODrawerItem;
import com.odoo.core.support.list.OCursorListAdapter;
import com.odoo.core.utils.BitmapUtils;
import com.odoo.core.utils.IntentUtils;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OCursorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Contact extends BaseFragment implements
        ISyncStatusObserverListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        OCursorListAdapter.OnViewBindListener,
        IOnSearchViewChangeListener,
        View.OnClickListener,
        AdapterView.OnItemClickListener  {

//    ISyncStatusObserverListener,
//    LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener,
//    OCursorListAdapter.OnViewBindListener, IOnSearchViewChangeListener, View.OnClickListener,
//    IOnItemClickListener, OBottomSheet.OSheetActionClickListener,
//    OBottomSheet.OSheetMenuCreateListener, OBottomSheet.OSheetItemClickListener

    public static final String TAG = ResPartner.class.getSimpleName();
    public static final String EXTRA_KEY_TYPE = "extra_key_type";

    // UI component
    @BindView(R.id.listview)
    ListView mList;

    // Odoo component
    private OCursorListAdapter mAdapter = null;

    // Android Component
    private View mView;
    private Bundle extras;

    // Java Component
    private String mCurFilter = null;
    private boolean syncRequested = false;
    private Contact.Type mType;
    public enum Type {
        Customer,
        Supplier,
        Company
    }

//    public static final String KEY = Contact.class.getSimpleName();
//    public static final String KEY_FILTER_REQUEST = "key_filter_request";
//    public static final String KEY_CUSTOMER_ID = "key_customer_id";
//    public static final String KEY_FILTER_TYPE = CRMLeads.KEY_MENU;
//    private View mView;
//    private String mCurFilter = null;
//    private ListView mPartnersList = null;
//    private OCursorListAdapter mAdapter = null;
//    private boolean syncRequested = false;

//    public enum Type {
//        Leads, Opportunities
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.common_listview, container, false);
        ButterKnife.bind(this, view);
        setHasOptionsMenu(true);
        setHasSyncStatusObserver(TAG, this, db());
        setHasFloatingButton(view, R.id.fabButton, mList, this);
        setHasSwipeRefreshView(view, R.id.swipe_container, this);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;

        // bind adapter
        mAdapter = new OCursorListAdapter(getActivity(), null, R.layout.contact_row_item);
        mAdapter.setOnViewBindListener(this);
        mAdapter.setHasSectionIndexers(true, "name");
        mList.setAdapter(mAdapter);
        mList.setFastScrollAlwaysVisible(true);
        mList.setOnItemClickListener(this);

        getLoaderManager().initLoader(0, null, this);

        extras = getArguments();
        if (extras != null && extras.getString(EXTRA_KEY_TYPE) != null){
            mType = Type.valueOf(extras.getString(EXTRA_KEY_TYPE));
        }
    }

    @Override
    public void onViewBind(View view, Cursor cursor, ODataRow row) {
        Bitmap bmp = getImage(row);
        OControls.setImage(view, R.id.image, bmp);
        OControls.setText(view, R.id.name, row.getString("name"));
        OControls.setText(view, R.id.company_name, (row.getString("company_name").equals("false"))
                ? "" : row.getString("company_name"));
        OControls.setText(view, R.id.email, (row.getString("email").equals("false") ? " "
                : row.getString("email")));
    }

    private Bitmap getImage(ODataRow row){
        Bitmap img;
        if (!row.getString("image").equals("false")) {
            img = BitmapUtils.getBitmapImage(getActivity(), row.getString("image"));
        } else if (!row.getString("image_medium").equals("false")) {
            img = BitmapUtils.getBitmapImage(getActivity(), row.getString("image_medium"));
        } else if (!row.getString("image_small").equals("false")){
            img = BitmapUtils.getBitmapImage(getActivity(), row.getString("image_small"));
        } else {
            img = BitmapUtils.getAlphabetImage(getActivity(), row.getString("name"));
        }
        return img;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        String where = null;
        List<String> args = new ArrayList<>();

        if (!TextUtils.isEmpty(mCurFilter)) {
            where = "(name like ? OR company_name like ?)";
            args.addAll(Arrays.asList(
                    "%" + mCurFilter + "%",
                    "%" + mCurFilter + "%"
            ));
        }

        if (mType!=null){
            switch (mType) {
                case Customer:
                    where += " AND (customer = ?)";
                    args.addAll(Arrays.asList("true"));
                    break;
                case Supplier:
                    where += " AND (supplier = ?)";
                    args.addAll(Arrays.asList("true"));
                    break;
                case Company:
                    where += " AND (is_company = ?)";
                    args.addAll(Arrays.asList("true"));
                    break;
            }
        }
        String selection = (args.size() > 0) ? where : null;
        String[] selectionArgs = (args.size() > 0) ? args.toArray(new String[args.size()]) : null;
        return new CursorLoader(getActivity(), db().uri(), null, selection, selectionArgs, "name COLLATE NOCASE ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
        if (data.getCount() > 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setVisible(mView, R.id.swipe_container);
                    OControls.setGone(mView, R.id.data_list_no_item);
                    setHasSwipeRefreshView(mView, R.id.swipe_container, Contact.this);
                }
            }, 500);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setGone(mView, R.id.swipe_container);
                    OControls.setVisible(mView, R.id.data_list_no_item);
                    setHasSwipeRefreshView(mView, R.id.data_list_no_item, Contact.this);
                    OControls.setImage(mView, R.id.icon, R.drawable.ic_action_customers);
                    OControls.setText(mView, R.id.title, _s(R.string.label_no_customer_found));
                    OControls.setText(mView, R.id.subTitle, "");
                }
            }, 500);
            if (db().isEmptyTable() && !syncRequested) {
                syncRequested = true;
                onRefresh();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    @Override
    public Class<ResPartner> database() {
        return ResPartner.class;
    }

    @Override
    public List<ODrawerItem> drawerMenus(Context context) {
        List<ODrawerItem> items = new ArrayList<ODrawerItem>();
        items.add(new ODrawerItem(TAG).setTitle("Contact")
                .setIcon(R.drawable.ic_action_customers)
                .setInstance(new Contact()));
        return items;
    }

    @Override
    public void onStatusChange(Boolean refreshing) {
        // Sync Status
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onRefresh() {
        if (inNetwork()) {
            parent().sync().requestSync(ResPartner.AUTHORITY);
            setSwipeRefreshing(true);
        } else {
            hideRefreshingProgress();
            Toast.makeText(getActivity(), _s(R.string.toast_network_required), Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_partners, menu);
        setHasSearchView(this, menu, R.id.menu_partner_search);
    }

    @Override
    public boolean onSearchViewTextChange(String newFilter) {
        mCurFilter = newFilter;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    @Override
    public void onSearchViewClose() {
        // nothing to do
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fabButton:
                loadActivity(null);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ODataRow row = OCursorUtils.toDatarow((Cursor) mAdapter.getItem(position));
        loadActivity(row);
    }

    private void loadActivity(ODataRow row) {
        Bundle data = new Bundle();
        if (row != null) {
            data = row.getPrimaryBundleData();
        }
        IntentUtils.startActivity(getActivity(), ContactDetails.class, data);
    }

//    @Override
//    public void onItemClick(View view, final int position) {
//        ODataRow row = OCursorUtils.toDatarow((Cursor) mAdapter.getItem(position));
//        if (row.getInt(OColumn.ROW_ID) == 0) {
//            CustomerQuickCreator customerQuickCreater =
//                    new CustomerQuickCreator(new OnLiveSearchRecordCreateListener() {
//                        @Override
//                        public void recordCreated(ODataRow row) {
//                            Cursor cr = getActivity().getContentResolver()
//                                    .query(db().uri(), null, "id = ?", new String[]{row.getString("id")}
//                                            , null);
//                            cr.moveToFirst();
//                            showSheet(cr);
//                        }
//                    });
//            customerQuickCreater.execute(row);
//        } else
//            showSheet((Cursor) mAdapter.getItem(position));
//    }

//    @Override
//    public void onSheetActionClick(OBottomSheet sheet, Object data) {
//        sheet.dismiss();
//        if (data instanceof Cursor) {
//            loadActivity(OCursorUtils.toDatarow((Cursor) data));
//        }
//    }

//    private void loadActivity(ODataRow row) {
//        Bundle data = new Bundle();
//        if (row != null) {
//            data = row.getPrimaryBundleData();
//        }
//        IntentUtils.startActivity(getActivity(), ProductDetail.class, data);
//    }

//    private void showSheet(Cursor data) {
//        OBottomSheet bottomSheet = new OBottomSheet(getActivity());
//        String title = data.getString(data.getColumnIndex("name"));
//        if (bottomSheet.isShowing()) {
//            bottomSheet.dismiss();
//        }
//        bottomSheet.setSheetActionsMenu(R.menu.menu_sheet_customer);
//        bottomSheet.setSheetTitle(title);
//        bottomSheet.setData(data);
//        bottomSheet.setActionIcon(R.drawable.ic_action_edit, this);
//        bottomSheet.setSheetMenuCreateListener(this);
//        bottomSheet.setSheetItemClickListener(this);
//        bottomSheet.show();
//    }

//    @Override
//    public void onSheetMenuCreate(Menu menu, Object o) {
//        ODataRow row = OCursorUtils.toDatarow((Cursor) o);
//        String address = ((ResPartner) db()).getAddress(row);
//        if (address.equals("false") || TextUtils.isEmpty(address)) {
//            menu.findItem(R.id.menu_customer_location).setVisible(false);
//        }
//        String contact = ResPartner.getContact(getActivity(), row.getInt(OColumn.ROW_ID));
//        if (contact.equals("false")) {
//            menu.findItem(R.id.menu_customer_call).setVisible(false);
//        }
//        if (row.getString("email").equals("false")) {
//            menu.findItem(R.id.menu_customer_send_message).setVisible(false);
//        }
//    }

//    @Override
//    public void onSheetItemClick(OBottomSheet sheet, MenuItem item, Object data) {
//        sheet.dismiss();
//        ODataRow row = OCursorUtils.toDatarow((Cursor) data);
//        switch (item.getItemId()) {
//            case R.id.menu_customer_opportunity:
//                requestOpportunity(row.getInt(OColumn.ROW_ID), row.getString("name"));
//                break;
//            case R.id.menu_customer_leads:
//                requestLeads(Type.Leads, row.getInt(OColumn.ROW_ID), row.getString("name"));
//                break;
//            case R.id.menu_customer_location:
//                String address = ((ResPartner) db()).getAddress(row);
//                if (!address.equals("false") && !TextUtils.isEmpty(address))
//                    IntentUtils.redirectToMap(getActivity(), address);
//                else
//                    Toast.makeText(getActivity(), _s(R.string.label_no_location_found), Toast.LENGTH_LONG).show();
//                break;
//            case R.id.menu_customer_call:
//                String contact = ResPartner.getContact(getActivity(), row.getInt(OColumn.ROW_ID));
//                if (!contact.equals("false"))
//                    IntentUtils.requestCall(getActivity(), contact);
//                else
//                    Toast.makeText(getActivity(), _s(R.string.label_no_contact_found), Toast.LENGTH_LONG).show();
//                break;
//            case R.id.menu_customer_send_message:
//                if (!row.getString("email").equals("false"))
//                    IntentUtils.requestMessage(getActivity(), row.getString("email"));
//                else
//                    Toast.makeText(getActivity(), _s(R.string.label_no_email_found), Toast.LENGTH_LONG).show();
//                break;
//            case R.id.menu_customer_schedule_call:
//                Bundle extra = row.getPrimaryBundleData();
//                extra.putInt(PhoneCallDetail.KEY_OPPORTUNITY_ID, -1);
//                extra.putBoolean(PhoneCallDetail.KEY_LOG_CALL_REQUEST, true);
//                IntentUtils.startActivity(getActivity(), PhoneCallDetail.class, extra);
//                break;
//        }
//    }

//    @Override
//    public Class<ResPartner> database() {
//        return ResPartner.class;
//    }

//    @Override
//    public void onRefresh() {
//        if (inNetwork()) {
//            parent().sync().requestSync(ResPartner.AUTHORITY);
//            setSwipeRefreshing(true);
//        } else {
//            hideRefreshingProgress();
//            Toast.makeText(getActivity(), _s(R.string.toast_network_required), Toast.LENGTH_LONG)
//                    .show();
//        }
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//        }
//        return super.onOptionsItemSelected(item);
//    }

//    private void requestOpportunity(int row_id, String name) {
//        Bundle extra = new Bundle();
//        extra.putBoolean(KEY_FILTER_REQUEST, true);
//        extra.putInt(KEY_CUSTOMER_ID, row_id);
//        extra.putString("name", name);
//        startFragment(new CRMOpportunitiesPager(), true, extra);
//    }

//    private void requestLeads(Type type, int row_id, String name) {
//        Bundle extra = new Bundle();
//        extra.putBoolean(KEY_FILTER_REQUEST, true);
//        extra.putInt(KEY_CUSTOMER_ID, row_id);
//        extra.putString(KEY_FILTER_TYPE, type.toString());
//        extra.putString("name", name);
//        startFragment(new CRMLeads(), true, extra);
//    }

//    @Override
//    public void onItemDoubleClick(View view, int position) {
//        final ODataRow row = OCursorUtils.toDatarow((Cursor) mAdapter.getItem(position));
//        if (row.getInt(OColumn.ROW_ID) == 0) {
//            CustomerQuickCreator customerQuickCreater =
//                    new CustomerQuickCreator(new OnLiveSearchRecordCreateListener() {
//                        @Override
//                        public void recordCreated(ODataRow row) {
//                            loadActivity(row);
//                        }
//                    });
//            customerQuickCreater.execute(row);
//        } else
//            loadActivity(row);
//    }
//
//    private void loadActivity(ODataRow row) {
//        Bundle data = null;
//        if (row != null) {
//            data = row.getPrimaryBundleData();
//        }
//        IntentUtils.startActivity(getActivity(), CustomerDetails.class, data);
//    }
//
//    private class CustomerQuickCreator extends AsyncTask<ODataRow, Void, ODataRow> {
//        private ProgressDialog progressDialog;
//        private OnLiveSearchRecordCreateListener mOnLiveSearchRecordCreateListener;
//
//        public CustomerQuickCreator(OnLiveSearchRecordCreateListener listener) {
//            mOnLiveSearchRecordCreateListener = listener;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            progressDialog = new ProgressDialog(getActivity());
//            progressDialog.setTitle(R.string.title_working);
//            progressDialog.setMessage(_s(R.string.title_please_wait));
//            progressDialog.setCancelable(false);
//            progressDialog.show();
//        }
//
//        @Override
//        protected ODataRow doInBackground(ODataRow... params) {
//            try {
//                Thread.sleep(500);
//                return db().quickCreateRecord(params[0]);
//            } catch (Exception e) {
//
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(ODataRow row) {
//            super.onPostExecute(row);
//            progressDialog.dismiss();
//            getLoaderManager().restartLoader(0, null, Contact.this);
//            if (mOnLiveSearchRecordCreateListener != null && row != null) {
//                mOnLiveSearchRecordCreateListener.recordCreated(row);
//            }
//        }
//    }
//
//    public interface OnLiveSearchRecordCreateListener {
//        public void recordCreated(ODataRow row);
//    }
}
