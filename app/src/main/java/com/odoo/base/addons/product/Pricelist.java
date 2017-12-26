package com.odoo.base.addons.product;

import android.content.Context;

import com.odoo.base.addons.res.ResCompany;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by fanani on 12/26/17.
 */

public class Pricelist extends OModel {

    OColumn name = new OColumn("Pricelist Name", OVarchar.class).setRequired();
    OColumn active = new OColumn("Active", OBoolean.class).setDefaultValue(true);
    //    item_ids = fields.One2many(
//            'product.pricelist.item', 'pricelist_id', 'Pricelist Items',
//    copy=True, default=_get_default_item_ids)
    OColumn currency_id = new OColumn("Currency", ResCurrency.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn company_id = new OColumn("Company", ResCompany.class, OColumn.RelationType.ManyToOne);
    OColumn sequence = new OColumn("Sequence", OInteger.class).setDefaultValue(16);
//    country_group_ids = fields.Many2many('res.country.group', 'res_country_group_pricelist_rel',
//            'pricelist_id', 'res_country_group_id', string='Country Groups')

    public Pricelist(Context context, OUser user) {
        super(context, "product.pricelist", user);
    }

}
