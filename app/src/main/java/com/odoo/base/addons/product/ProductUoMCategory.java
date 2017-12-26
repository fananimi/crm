package com.odoo.base.addons.product;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by fanani on 12/26/17.
 */

public class ProductUoMCategory extends OModel {

    OColumn name = new OColumn("Name", OVarchar.class).setRequired();

    public ProductUoMCategory(Context context, OUser user) {
        super(context, "product.uom.categ", user);
    }

}
