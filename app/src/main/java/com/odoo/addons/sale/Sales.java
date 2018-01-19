package com.odoo.addons.sale;

import android.content.Context;
import android.database.Cursor;
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
import com.odoo.base.addons.sale.SaleOrder;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.support.addons.fragment.BaseFragment;
import com.odoo.core.support.addons.fragment.IOnSearchViewChangeListener;
import com.odoo.core.support.addons.fragment.ISyncStatusObserverListener;
import com.odoo.core.support.drawer.ODrawerItem;
import com.odoo.core.support.list.OCursorListAdapter;
import com.odoo.core.utils.IntentUtils;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OCursorUtils;
import com.odoo.core.utils.OResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by fanani on 12/26/17.
 */

public class Sales extends BaseFragment implements
        ISyncStatusObserverListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        OCursorListAdapter.OnViewBindListener,
        IOnSearchViewChangeListener,
        View.OnClickListener,
        AdapterView.OnItemClickListener {

    public static final String TAG = Sales.class.getSimpleName();
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
    private Sales.Type mType;
    public enum Type {
        Quotation,
        SaleOrder
    }

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
        mAdapter = new OCursorListAdapter(getActivity(), null, R.layout.sale_row_item);
        mAdapter.setOnViewBindListener(this);
        // mAdapter.setHasSectionIndexers(true, "name");
        mList.setAdapter(mAdapter);
        mList.setFastScrollAlwaysVisible(true);
        mList.setOnItemClickListener(this);

        getLoaderManager().initLoader(0, null, this);

        extras = getArguments();
        if (extras != null && extras.getString(EXTRA_KEY_TYPE) != null){
            mType = Type.valueOf(extras.getString(EXTRA_KEY_TYPE));
            switch (mType){
                case Quotation:
                    OControls.setVisible(mView, R.id.fabButton);
                    break;
                case SaleOrder:
                    OControls.setGone(mView, R.id.fabButton);
                    break;
            }
        }
    }

    @Override
    public void onViewBind(View view, Cursor cursor, ODataRow row) {
        OControls.setText(view, R.id.name, row.getString("name"));
        OControls.setText(view, R.id.order_line_count, row.getString("order_line_count"));
//        OControls.setText(view, R.id.date_order, row.getString("date_order"));
        OControls.setText(view, R.id.partner_name, row.getString("partner_name"));
        OControls.setText(view, R.id.state_title, row.getString("state_title"));
        OControls.setText(view, R.id.amount_total, row.getString("amount_total"));
        OControls.setText(view, R.id.currency_symbol, row.getString("currency_symbol"));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        String where = null;
        List<String> args = new ArrayList<>();

        if (mType!=null){
            switch (mType) {
                case Quotation:
                    where = "(state = ? OR state = ? OR state = ?)";
                    args.addAll(Arrays.asList(
                            SaleOrder.STATE.draft.toString(),
                            SaleOrder.STATE.sent.toString(),
                            SaleOrder.STATE.cancel.toString()
                    ));
                    break;
                case SaleOrder:
                    where = "(state = ? OR state = ?)";
                    args.addAll(Arrays.asList(
                            SaleOrder.STATE.sale.toString(),
                            SaleOrder.STATE.done.toString()
                    ));
                    break;
            }
        }

        if (!TextUtils.isEmpty(mCurFilter)) {
            where += " AND (name LIKE ? OR partner_name LIKE ?)";
            args.addAll(Arrays.asList(
                    "%" + mCurFilter + "%",
                    "%" + mCurFilter + "%"
            ));
        }

        String selection = (args.size() > 0) ? where : null;
        String[] selectionArgs = (args.size() > 0) ? args.toArray(new String[args.size()]) : null;
        return new CursorLoader(getActivity(), db().uri(), null, selection, selectionArgs, "date_order DESC");
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
                    setHasSwipeRefreshView(mView, R.id.swipe_container, Sales.this);
                }
            }, 500);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setGone(mView, R.id.swipe_container);
                    OControls.setVisible(mView, R.id.data_list_no_item);
                    setHasSwipeRefreshView(mView, R.id.data_list_no_item, Sales.this);
                    OControls.setImage(mView, R.id.icon,
                            (mType == Type.Quotation) ? R.drawable.ic_action_quotation : R.drawable.ic_action_sale_order);
                    OControls.setText(mView, R.id.title, "No " + mType + " Found");
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
    public Class<SaleOrder> database() {
        return SaleOrder.class;
    }

    @Override
    public List<ODrawerItem> drawerMenus(Context context) {
        List<ODrawerItem> menu = new ArrayList<>();
        menu.add(new ODrawerItem(TAG).setTitle(OResource.string(context, R.string.label_quotation))
                .setIcon(R.drawable.ic_action_quotation)
                .setInstance(new Sales())
                .setExtra(data(Type.Quotation)));
        menu.add(new ODrawerItem(TAG).setTitle(OResource.string(context, R.string.label_sale_orders))
                .setIcon(R.drawable.ic_action_sale_order)
                .setInstance(new Sales())
                .setExtra(data(Type.SaleOrder)));
        return menu;
    }

    private Bundle data(Type type) {
        Bundle extra = new Bundle();
        extra.putString(EXTRA_KEY_TYPE, type.toString());
        return extra;
    }

    @Override
    public void onStatusChange(Boolean refreshing) {
        // Sync Status
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onRefresh() {
        if (inNetwork()) {
            parent().sync().requestSync(SaleOrder.AUTHORITY);
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
        inflater.inflate(R.menu.menu_sales_order, menu);
        setHasSearchView(this, menu, R.id.menu_sales_search);
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
        data.putString(EXTRA_KEY_TYPE, extras.getString(EXTRA_KEY_TYPE));
        IntentUtils.startActivity(getActivity(), SaleDetail.class, data);
    }

}

