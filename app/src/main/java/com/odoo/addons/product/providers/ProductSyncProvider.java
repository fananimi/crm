package com.odoo.addons.product.providers;

import com.odoo.base.addons.product.ProductTemplate;
import com.odoo.core.orm.provider.BaseModelProvider;

/**
 * Created by fanani on 12/26/17.
 */

public class ProductSyncProvider extends BaseModelProvider {

    public static final String TAG = ProductSyncProvider.class.getSimpleName();

    @Override
    public String authority() {
        return ProductTemplate.AUTHORITY;
    }

}
