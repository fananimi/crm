package com.odoo.addons.product.services;

import android.content.Context;
import android.os.Bundle;

import com.odoo.base.addons.product.ProductTemplate;
import com.odoo.core.service.OSyncAdapter;
import com.odoo.core.service.OSyncService;
import com.odoo.core.support.OUser;

/**
 * Created by fanani on 12/26/17.
 */

public class ProductSyncService extends OSyncService {

    public static final String TAG = ProductSyncService.class.getSimpleName();

    @Override
    public OSyncAdapter getSyncAdapter(OSyncService service, Context context) {
        return new OSyncAdapter(context, ProductTemplate.class, service, true);
    }

    @Override
    public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {
    }

}
