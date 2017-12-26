package com.odoo.base.addons.product;

import android.content.Context;

import com.odoo.base.addons.res.ResCompany;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBlob;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by fanani on 12/26/17.
 */

public class ProductProduct extends OModel {

    // ProductTemplate Fields
    OColumn name = new OColumn("Name", OVarchar.class).setRequired();
    OColumn sequence = new OColumn("Sequence", OInteger.class).setDefaultValue(1);
    OColumn description = new OColumn("Description", OText.class);
    OColumn description_purchase = new OColumn("Purchase Description", OText.class);
    OColumn description_sale = new OColumn("Sale Description", OText.class);
    OColumn type = new OColumn("Product Type", OSelection.class)
            .addSelection("consu", "Consumable")
            .addSelection("service", "Service")
            .setDefaultValue("consu")
            .setRequired();
    OColumn rental = new OColumn("Can be Rent", OBoolean.class).setDefaultValue(false);
    OColumn categ_id = new OColumn("Internal Category", ProductCategory.class, OColumn.RelationType.ManyToOne)
            .setRequired();
    OColumn currency_id = new OColumn("Currenc", ResCurrency.class, OColumn.RelationType.ManyToOne);
    OColumn list_price = new OColumn("Sale Price", OFloat.class).setDefaultValue(0.0).setRequired();
    OColumn warranty = new OColumn("warranty", OFloat.class).setDefaultValue(0.0);
    OColumn sale_ok = new OColumn("Can be Sold", OBoolean.class).setDefaultValue(true);
    OColumn purchase_ok = new OColumn("Can be Purchased", OBoolean.class).setDefaultValue(true);
    OColumn pricelist_id = new OColumn("Pricelist", Pricelist.class, OColumn.RelationType.ManyToOne);
    OColumn company_id = new OColumn("Company", ResCompany.class, OColumn.RelationType.ManyToOne);
    OColumn color = new OColumn("Color Index", OInteger.class).setDefaultValue(0);

    // ProductProduct Field
    OColumn price = new OColumn("Price", OFloat.class).setRequired();
    OColumn price_extra = new OColumn("Variant Price Extra", OFloat.class);
    OColumn lst_price = new OColumn("Sale Price", OFloat.class);
    OColumn default_code = new OColumn("Internal Reference", OVarchar.class);
    OColumn code = new OColumn("Internal Reference", OVarchar.class);
    OColumn partner_ref = new OColumn("Customer Ref", OVarchar.class);
    OColumn active = new OColumn("active", OBoolean.class).setDefaultValue(true);
    OColumn product_tmpl_id = new OColumn("Product Template", ProductTemplate.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn barcode = new OColumn("Barcode", OVarchar.class);
    //    attribute_value_ids = fields.Many2many(
//            'product.attribute.value', string='Attributes', ondelete='restrict')
//            # image: all image fields are base64 encoded and PIL-supported
//            image_variant = fields.Binary(
//            "Variant Image", attachment=True,
//            help="This field holds the image used as image for the product variant, limited to 1024x1024px.")
    OColumn image = new OColumn("Big-sized image", OBlob.class);
    OColumn image_medium= new OColumn("Small-sized image", OBlob.class);
    OColumn image_small = new OColumn("Medium-sized image", OBlob.class);
    OColumn standard_price = new OColumn("Cost", OFloat.class);
    OColumn volume = new OColumn("Volume", OFloat.class);
    OColumn weight = new OColumn("Weight", OFloat.class);
//    pricelist_item_ids = fields.Many2many(
//            'product.pricelist.item', 'Pricelist Items', compute='_get_pricelist_items')

    public ProductProduct(Context context, OUser user) {
        super(context, "product.product", user);
    }

}
