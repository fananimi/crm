package com.odoo.base.addons.product;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by fanani on 12/26/17.
 */

public class ProductUoM extends OModel {

    OColumn name = new OColumn("Unit of Measure", OVarchar.class).setRequired();
    OColumn category_id = new OColumn("Category", ProductUoMCategory.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn factor = new OColumn("Ratio", OFloat.class).setDefaultValue(1.0).setRequired();
    OColumn factor_inv = new OColumn("Bigger Ratio", OFloat.class).setRequired();
    OColumn rounding = new OColumn("Rounding Precision", OFloat.class).setDefaultValue(0.01).setRequired();
    OColumn active = new OColumn("active", OBoolean.class).setDefaultValue(true);
    OColumn uom_type = new OColumn("Type", OSelection.class)
            .addSelection("bigger", "Bigger than the reference Unit of Measure")
            .addSelection("reference", "Reference Unit of Measure for this category")
            .addSelection("smaller", "Smaller than the reference Unit of Measure")
            .setDefaultValue("reference")
            .setRequired();

    public ProductUoM(Context context, OUser user) {
        super(context, "product.uom", user);
    }

}
