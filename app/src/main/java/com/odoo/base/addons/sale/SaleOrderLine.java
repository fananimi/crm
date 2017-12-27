package com.odoo.base.addons.sale;

import android.content.Context;

import com.odoo.base.addons.product.ProductProduct;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.support.OUser;

/**
 * Created by fanani on 12/27/17.
 */

public class SaleOrderLine extends OModel {

    OColumn order_id = new OColumn("Order Reference", SaleOrder.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn name = new OColumn("Description", OText.class).setRequired();
    OColumn sequence = new OColumn("Sequence", OInteger.class).setDefaultValue(10);
    //    invoice_lines = fields.Many2many('account.invoice.line', 'sale_order_line_invoice_rel', 'order_line_id', 'invoice_line_id', string='Invoice Lines', copy=False)
//    invoice_status = fields.Selection([
//            ('upselling', 'Upselling Opportunity'),
//            ('invoiced', 'Fully Invoiced'),
//            ('to invoice', 'To Invoice'),
//            ('no', 'Nothing to Invoice')
//            ], string='Invoice Status', compute='_compute_invoice_status', store=True, readonly=True, default='no')
    OColumn price_unit = new OColumn("Unit Price", OFloat.class).setRequired().setDefaultValue(0.0);
    OColumn price_subtotal = new OColumn("Subtotal", OFloat.class).setRequired().setDefaultValue(0.0);
    OColumn price_tax = new OColumn("Taxes", OFloat.class);
    OColumn price_total = new OColumn("Taxes", OFloat.class);
    OColumn price_reduce = new OColumn("Taxes", OFloat.class);
    //    tax_id = fields.Many2many('account.tax', string='Taxes', domain=['|', ('active', '=', False), ('active', '=', True)])
//    price_reduce_taxinc = fields.Monetary(compute='_get_price_reduce_tax', string='Price Reduce Tax inc', readonly=True, store=True)
//    price_reduce_taxexcl = fields.Monetary(compute='_get_price_reduce_notax', string='Price Reduce Tax excl', readonly=True, store=True)
    OColumn discount = new OColumn("Discount (%)", OFloat.class).setDefaultValue(0.0);
    OColumn product_id = new OColumn("Product", ProductProduct.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn product_uom_qty = new OColumn("Quantity", OFloat.class).setDefaultValue(1.0).setRequired();
    //    OColumn product_uom = new OColumn("Unit of Measure", ProductUoM.class).setRequired();
//    qty_delivered_updateable = fields.Boolean(compute='_compute_qty_delivered_updateable', string='Can Edit Delivered', readonly=True, default=True)
//    qty_delivered = fields.Float(string='Delivered', copy=False, digits=dp.get_precision('Product Unit of Measure'), default=0.0)
//    qty_to_invoice = fields.Float(
//    compute='_get_to_invoice_qty', string='To Invoice', store=True, readonly=True,
//    digits=dp.get_precision('Product Unit of Measure'))
//    qty_invoiced = fields.Float(
//    compute='_get_invoice_qty', string='Invoiced', store=True, readonly=True,
//    digits=dp.get_precision('Product Unit of Measure'))
//
//    salesman_id = fields.Many2one(related='order_id.user_id', store=True, string='Salesperson', readonly=True)
//    currency_id = fields.Many2one(related='order_id.currency_id', store=True, string='Currency', readonly=True)
//    company_id = fields.Many2one(related='order_id.company_id', string='Company', store=True, readonly=True)
//    order_partner_id = fields.Many2one(related='order_id.partner_id', store=True, string='Customer')
//    analytic_tag_ids = fields.Many2many('account.analytic.tag', string='Analytic Tags')
    OColumn state = new OColumn("Order Status", OSelection.class)
            .addSelection("draft", "Quotation")
            .addSelection("sent", "Quotation Sent")
            .addSelection("done", "Sale Order")
            .addSelection("cancel", "Cancelled")
            .setDefaultValue("draft")
            .setRequired();
//    customer_lead = fields.Float(
//            'Delivery Lead Time', required=True, default=0.0,
//    help="Number of days between the order confirmation and the shipping of the products to the customer", oldname="delay")
//    procurement_ids = fields.One2many('procurement.order', 'sale_line_id', string='Procurements')
//
//    layout_category_id = fields.Many2one('sale.layout_category', string='Section')
//    layout_category_sequence = fields.Integer(string='Layout Sequence')
//            # TODO: remove layout_category_sequence in master or make it work properly

    public SaleOrderLine(Context context, OUser user) {
        super(context, "sale.order.line", user);
    }
}
