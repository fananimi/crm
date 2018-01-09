package com.odoo.base.addons.sale;

import android.content.Context;

import com.odoo.BuildConfig;
import com.odoo.base.addons.product.Pricelist;
import com.odoo.base.addons.product.ProductProduct;
import com.odoo.base.addons.res.ResCompany;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.OvaluesUtils;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.ODate;
import com.odoo.core.orm.fields.types.ODateTime;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OSelection;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

import org.json.JSONArray;

import java.util.HashMap;

/**
 * Created by fanani on 12/27/17.
 */

public class SaleOrder extends OModel {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.sale_order";

    public enum STATE {
        draft, sent, sale, done, cancel
    }

    OColumn name = new OColumn("Order Reference", OVarchar.class).setRequired();
    OColumn origin = new OColumn("Source Document", OVarchar.class);
    OColumn client_order_ref = new OColumn("Customer Reference", OVarchar.class);
    OColumn state = new OColumn("Status", OSelection.class)
            .addSelection(STATE.draft.toString(), "Quotation")
            .addSelection(STATE.sent.toString(), "Quotation Sent")
            .addSelection(STATE.sale.toString(), "Sales Order")
            .addSelection(STATE.done.toString(), "Done")
            .addSelection(STATE.cancel.toString(), "Cancelled")
            .setDefaultValue(STATE.draft.toString())
            .setRequired();
    OColumn date_order = new OColumn("Order Date", ODateTime.class).setRequired();
    OColumn validity_date = new OColumn("Expiration Date", ODate.class);
    OColumn confirmation_date = new OColumn("Confirmation Date", ODateTime.class);
    OColumn user_id = new OColumn("Salesperson", ResUsers.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn partner_id = new OColumn("Customer", ResPartner.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn partner_invoice_id = new OColumn("Invoice Address", ResPartner.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn partner_shipping_id = new OColumn("Delivery Address", ResPartner.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn pricelist_id = new OColumn("Pricelist", Pricelist.class, OColumn.RelationType.ManyToOne).setRequired();
    OColumn currency_id = new OColumn("Currency", ResCurrency.class, OColumn.RelationType.ManyToOne).setRequired();
    //    project_id = fields.Many2one('account.analytic.account', 'Analytic Account', readonly=True, states={'draft': [('readonly', False)], 'sent': [('readonly', False)]}, help="The analytic account related to a sales order.", copy=False)
//    related_project_id = fields.Many2one('account.analytic.account', inverse='_inverse_project_id', related='project_id', string='Analytic Account', help="The analytic account related to a sales order.")
    OColumn order_line = new OColumn("Order Lines", SaleOrderLine.class, OColumn.RelationType.OneToMany)
            .setRelatedColumn("order_id");
    OColumn invoice_count = new OColumn("# of Invoices", OInteger.class);
    //    invoice_ids = fields.Many2many("account.invoice", string='Invoices', compute="_get_invoiced", readonly=True, copy=False)
    OColumn invoice_status = new OColumn("Invoice Status", OSelection.class)
            .addSelection("upselling", "Upselling Opportunity")
            .addSelection("invoiced", "Fully Invoiced")
            .addSelection("to invoice", "To Invoice")
            .addSelection("no", "Nothing to Invoice")
            .setDefaultValue("draft");
    OColumn note = new OColumn("Terms and conditions", OText.class);
    OColumn amount_untaxed = new OColumn("Untaxed Amount", OFloat.class);
    OColumn amount_tax = new OColumn("Taxes", OFloat.class);
    OColumn amount_total  = new OColumn("Total", OFloat.class);
    //    payment_term_id = fields.Many2one('account.payment.term', string='Payment Terms', oldname='payment_term')
//    fiscal_position_id = fields.Many2one('account.fiscal.position', oldname='fiscal_position', string='Fiscal Position')
//    OColumn company_id = new OColumn("Company", ResCompany.class);
//    team_id = fields.Many2one('crm.team', 'Sales Team', change_default=True, default=_get_default_team, oldname='section_id')
//    procurement_group_id = fields.Many2one('procurement.group', 'Procurement Group', copy=False)
    OColumn product_id = new OColumn("ProductProduct", ProductProduct.class, OColumn.RelationType.ManyToOne);

    // Local columns
    @Odoo.Functional(method = "countOrderLines", store = true, depends = {"order_line"})
    OColumn order_line_count = new OColumn("Total Lines", OVarchar.class)
            .setLocalColumn();
    @Odoo.Functional(method = "storePartnerName", store = true, depends = {"partner_id"})
    OColumn partner_name = new OColumn("Customer Name", OVarchar.class)
            .setLocalColumn();
    @Odoo.Functional(method = "storeStateTitle", store = true, depends = {"state"})
    OColumn state_title = new OColumn("State Title", OVarchar.class)
            .setLocalColumn();
    @Odoo.Functional(method = "storeCurrencySymbol", store = true, depends = {"currency_id"})
    OColumn currency_symbol = new OColumn("Currency Symbol", OVarchar.class).setLocalColumn();

    public SaleOrder(Context context, OUser user) {
        super(context, "sale.order", user);
        setHasMailChatter(true);
    }

    public static STATE getState(ODataRow row){
        STATE state = (row == null) ? null : STATE.valueOf(row.getString("state"));
        return state;
    }

    public String countOrderLines(OValues values) {
        String results;
        JSONArray order_line = OvaluesUtils.getJSONArray(values, "order_line");
        if (order_line != null && order_line.length() > 0){
            String info;
            if (order_line.length() > 1){
                info = "lines";
            } else {
                info = "line";
            }
            results = "(" + order_line.length() + " " + info + ")";
        } else {
            results = "(No lines)";
        }
        return results;
    }

    public String storePartnerName(OValues values) {
        return OvaluesUtils.getValue(values, "partner_id", 1, null);
    }

    public String storeStateTitle(OValues row) {
        HashMap<String, String> mStates = new HashMap<String, String>();
        mStates.put("draft", "Quotation");
        mStates.put("sent", "Quotation Sent");
        mStates.put("sale", "Sales Order");
        mStates.put("done", "Done");
        mStates.put("cancel", "Cancelled");
        return mStates.get(row.getString("state"));
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

    public String getDefaultCurrencySymbol(){
        Context ctx = getContext();
        OUser user = OUser.current(ctx);
        int currency_id = user.getCompanyId();
        ResCompany resCompany = new ResCompany(ctx, null);
        ODataRow company = resCompany.browse(currency_id);
        ODataRow currency = company.getM2ORecord("currency_id").browse();
        return currency.getString("symbol");
    }

}
