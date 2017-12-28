package com.odoo.base.addons.product;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by fanani on 12/26/17.
 */

public class ProductCategory extends OModel {

    OColumn name = new OColumn("Name", OVarchar.class).setRequired();
    OColumn parent_id = new OColumn("Parent Category", ProductCategory.class, OColumn.RelationType.ManyToOne);
    OColumn child_id = new OColumn("Child Categories", ProductCategory.class, OColumn.RelationType.ManyToOne);
    OColumn type = new OColumn("Product Type", OSelection.class)
            .addSelection("view", "View")
            .addSelection("normal", "Normal")
            .setDefaultValue("Normal");
    OColumn parent_left = new OColumn("Left Parent", OInteger.class).setDefaultValue(1);
    OColumn product_count = new OColumn("# Products", OInteger.class);

    public ProductCategory(Context context, OUser user) {
        super(context, "product.category", user);
    }

}
