package com.odoo.base.addons.product;

import android.content.Context;
import android.util.Log;

import com.odoo.BuildConfig;
import com.odoo.base.addons.res.ResCompany;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.core.OvaluesUtils;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBlob;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.support.OUser;

/**
 * Created by fanani on 12/26/17.
 */

public class ProductTemplate extends OModel {

    private static final String TAG = ProductTemplate.class.getSimpleName();
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.product.template";

    OColumn name = new OColumn("Name", OVarchar.class).setRequired();
    OColumn sequence = new OColumn("Sequence", OInteger.class).setDefaultValue(1);
    OColumn description = new OColumn("Description", OText.class);
    OColumn description_purchase = new OColumn("Purchase Description", OText.class);
    OColumn description_sale = new OColumn("Sale Description", OText.class);
    OColumn type = new OColumn("Product Type", OSelection.class)
            .addSelection("product", "Stockable Product")
            .addSelection("consu", "Consumable")
            .addSelection("service", "Service")
            .setDefaultValue("product")
            .setRequired();
    OColumn rental = new OColumn("Can be Rent", OBoolean.class).setDefaultValue(false);
    OColumn categ_id = new OColumn("Internal Category", ProductCategory.class, OColumn.RelationType.ManyToOne)
            .setRequired();
    OColumn currency_id = new OColumn("Currenc", ResCurrency.class, OColumn.RelationType.ManyToOne);
    OColumn price = new OColumn("Price", OFloat.class).setDefaultValue(0.0);
    OColumn list_price = new OColumn("Sale Price", OFloat.class).setDefaultValue(0.0).setRequired();
    OColumn lst_price = new OColumn("Public Price", OFloat.class).
            setDefaultValue(0.0)
            .setRelatedColumn("list_price");
    OColumn standard_price = new OColumn("Cost", OFloat.class).setDefaultValue(0.0).setRequired();
    OColumn volume = new OColumn("Cost", OFloat.class).setDefaultValue(0.0);
    OColumn weight = new OColumn("Weight", OFloat.class).setDefaultValue(0.0);
    OColumn warranty = new OColumn("warranty", OFloat.class).setDefaultValue(0.0);
    OColumn sale_ok = new OColumn("Can be Sold", OBoolean.class).setDefaultValue(true);
    OColumn purchase_ok = new OColumn("Can be Purchased", OBoolean.class).setDefaultValue(true);
    OColumn pricelist_id = new OColumn("Pricelist", Pricelist.class, OColumn.RelationType.ManyToOne);
    //    uom_id = fields.Many2one(
//            'product.uom', 'Unit of Measure',
//    default=_get_default_uom_id, required=True,
//    help="Default Unit of Measure used for all stock operation.")
//    uom_po_id = fields.Many2one(
//            'product.uom', 'Purchase Unit of Measure',
//    default=_get_default_uom_id, required=True,
//    help="Default Unit of Measure used for purchase orders. It must be in the same category than the default unit of measure.")
    OColumn company_id = new OColumn("Company", ResCompany.class, OColumn.RelationType.ManyToOne);
    //    packaging_ids = fields.One2many(
//            'product.packaging', 'product_tmpl_id', 'Logistical Units',
//    help="Gives the different ways to package the same product. This has no impact on "
//            "the picking order and is mainly used if you use the EDI module.")
//    seller_ids = fields.One2many('product.supplierinfo', 'product_tmpl_id', 'Vendors')
    OColumn active = new OColumn("Active", OBoolean.class).setDefaultValue(true);
    OColumn color = new OColumn("Color Index", OInteger.class).setDefaultValue(0);
    //    attribute_line_ids = fields.One2many('product.attribute.line', 'product_tmpl_id', 'Product Attributes')
//    product_variant_ids = fields.One2many('product.product', 'product_tmpl_id', 'Products', required=True)
//            # performance: product_variant_id provides prefetching on the first product variant only
//            product_variant_id = fields.Many2one('product.product', 'Product', compute='_compute_product_variant_id')
//
//    product_variant_count = fields.Integer(
//            '# Product Variants', compute='_compute_product_variant_count')
//
//            # related to display product product information if is_product_variant
//            barcode = fields.Char('Barcode', oldname='ean13', related='product_variant_ids.barcode')
    OColumn default_code = new OColumn("Internal Reference", OVarchar.class);
    //    item_ids = fields.One2many('product.pricelist.item', 'product_tmpl_id', 'Pricelist Items')
    OColumn image = new OColumn("Big-sized image", OBlob.class);
    OColumn image_medium= new OColumn("Small-sized image", OBlob.class);
    OColumn image_small = new OColumn("Medium-sized image", OBlob.class);

    // Local columns
    @Odoo.Functional(method = "storeCurrencySymbol", store = true, depends = {"currency_id"})
    OColumn currency_symbol = new OColumn("Currency Symbol", OVarchar.class).setLocalColumn();

    public ProductTemplate(Context context, OUser user) {
        super(context, "product.template", user);
        setHasMailChatter(true);
    }

    public String storeCurrencySymbol(OValues values) {
        int currency_id = (int) Float.parseFloat(OvaluesUtils.getValue(values, "currency_id", 0, null));
        String selection = "(id) = ?";
        String [] selectionArgs = {Integer.toString(currency_id)};
        ResCurrency cr = new ResCurrency(getContext(), null);
        ODataRow row = cr.browse(null, selection, selectionArgs);
        String symbol = "";
        if (row != null){
            symbol = row.getString("symbol");
        }
        return symbol;
    }

    public static ORecordValues valuesToData(OValues values) {
        ORecordValues data = new ORecordValues();
        for (String key: values.keys()){
            data.put(key, values.get(key));
            Log.d(TAG, "ORecordValues : " + key + "=" + values.get(key).toString());
        }
        return data;
    }

}
