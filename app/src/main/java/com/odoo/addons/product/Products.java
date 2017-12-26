package com.odoo.addons.product;

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
import com.odoo.base.addons.product.ProductTemplate;
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

/**
 * Created by fanani on 12/26/17.
 */

public class Products extends BaseFragment implements
        ISyncStatusObserverListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        OCursorListAdapter.OnViewBindListener,
        IOnSearchViewChangeListener,
        View.OnClickListener,
        AdapterView.OnItemClickListener {

    public static final String KEY = Products.class.getSimpleName();
    public static final String EXTRA_KEY_TYPE = "extra_key_type";

    // UI component
    @BindView(R.id.listview)
    ListView mProductsList;

    // Odoo component
    private OCursorListAdapter mAdapter = null;

    // Android Component
    private View mView;
    private Bundle extras;

    // Java Component
    private String mCurFilter = null;
    private boolean syncRequested = false;
    private Products.Type mType;
    public enum Type {
        Product,
        Consumable,
        Service
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.common_listview, container, false);
        ButterKnife.bind(this, view);
        setHasOptionsMenu(true);
        setHasSyncStatusObserver(KEY, this, db());
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasSwipeRefreshView(view, R.id.swipe_container, this);
        mView = view;

        // bind adapter
        mAdapter = new OCursorListAdapter(getActivity(), null, R.layout.product_row_item);
        mAdapter.setOnViewBindListener(this);
        mAdapter.setHasSectionIndexers(true, "name");
        mProductsList.setAdapter(mAdapter);
        mProductsList.setFastScrollAlwaysVisible(true);
        mProductsList.setOnItemClickListener(this);

        setHasFloatingButton(view, R.id.fabButton, mProductsList, this);
        getLoaderManager().initLoader(0, null, this);

        extras = getArguments();
        if (extras != null && extras.getString(EXTRA_KEY_TYPE) != null){
            mType = Type.valueOf(extras.getString(EXTRA_KEY_TYPE));
        }
    }

    @Override
    public void onViewBind(View view, Cursor cursor, ODataRow row) {
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
        OControls.setImage(view, R.id.image, img);
        OControls.setText(view, R.id.name, row.getString("name"));
        OControls.setText(view, R.id.currency_symbol, row.getString("currency_symbol"));
        OControls.setText(view, R.id.list_price, row.getString("list_price"));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        String where = null;
        List<String> args = new ArrayList<>();

        if (!TextUtils.isEmpty(mCurFilter)) {
            where = "(name like ?)";
            args.addAll(Arrays.asList(new String[]{"%" + mCurFilter + "%"}));
        }

        if (mType!=null){
            switch (mType) {
                case Product:
                    where += " AND (type = ?)";
                    args.addAll(Arrays.asList(new String[]{"product"}));
                    break;
                case Consumable:
                    where += " AND (type = ?)";
                    args.addAll(Arrays.asList(new String[]{"consu"}));
                    break;
                case Service:
                    where += " AND type = ?";
                    args.addAll(Arrays.asList(new String[]{"service"}));
                    break;
            }
        }
        String selection = (args.size() > 0) ? where : null;
        String[] selectionArgs = (args.size() > 0) ? args.toArray(new String[args.size()]) : null;
        return new CursorLoader(getActivity(), db().uri(), null, selection, selectionArgs, "name");
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
                    setHasSwipeRefreshView(mView, R.id.swipe_container, Products.this);
                }
            }, 500);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setGone(mView, R.id.swipe_container);
                    OControls.setVisible(mView, R.id.data_list_no_item);
                    setHasSwipeRefreshView(mView, R.id.data_list_no_item, Products.this);
                    OControls.setImage(mView, R.id.icon, R.drawable.ic_shopping_basket_white_24dp);
                    OControls.setText(mView, R.id.title, _s(R.string.label_no_catalogue_found));
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
    public Class<ProductTemplate> database() {
        return ProductTemplate.class;
    }

    @Override
    public List<ODrawerItem> drawerMenus(Context context) {
        List<ODrawerItem> items = new ArrayList<>();
        items.add(new ODrawerItem(KEY).setTitle("Catalogues")
                .setIcon(R.drawable.ic_shopping_basket_white_24dp)
                .setInstance(new Products()));
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
            parent().sync().requestSync(ProductTemplate.AUTHORITY);
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
        inflater.inflate(R.menu.menu_products, menu);
        setHasSearchView(this, menu, R.id.menu_product_search);
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
        IntentUtils.startActivity(getActivity(), ProductDetail.class, data);
    }

}

