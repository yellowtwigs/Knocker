package com.yellowtwigs.knockin.controller.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.*
import com.yellowtwigs.knockin.Adapter.MyProductAdapter

import com.yellowtwigs.knockin.R

import java.util.Arrays

class PremiumActivity : AppCompatActivity(), PurchasesUpdatedListener{

    private var billingClient: BillingClient? = null
    private var loadProduct: AppCompatButton? = null
    private var recyclerProduct: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium)

        setupBillingClient()

        //View
        loadProduct = findViewById(R.id.btn_load_product)
        recyclerProduct = findViewById(R.id.recycler_product)
        recyclerProduct!!.setHasFixedSize(true)
        recyclerProduct!!.layoutManager = LinearLayoutManager(this)

        //Event
        loadProduct!!.setOnClickListener {
            if (billingClient!!.isReady) {
                val params = SkuDetailsParams.newBuilder()
                        .setSkusList(listOf("contacts_vip_unlimited"))
                        .setSkusList(listOf("custom_notifications_sound"))
                        .setType(BillingClient.SkuType.INAPP)
                        .build()

                billingClient!!.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
                    if (billingResult!!.responseCode == BillingClient.BillingResponseCode.OK) {
                        loadProductToRecyclerView(skuDetailsList)
                    } else {
                        Toast.makeText(this@PremiumActivity, "Cannot query product", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this@PremiumActivity, "Billing client not ready", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProductToRecyclerView(skuDetailsList: List<SkuDetails>) {
        val adapter = MyProductAdapter(this, skuDetailsList, billingClient)
        recyclerProduct!!.adapter = adapter
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(this)
                .build()

        billingClient!!.startConnection(object : BillingClientStateListener {
            /**
             * Called to notify that setup is complete.
             *
             * @param billingResult The [BillingResult] which returns the status of the setup process.
             */
            override fun onBillingSetupFinished(billingResult: BillingResult?) {
                if (billingResult!!.responseCode == BillingClient.BillingResponseCode.OK)
                    Toast.makeText(this@PremiumActivity, "Success to connect Billing", Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this@PremiumActivity, "" + billingResult.responseCode, Toast.LENGTH_SHORT).show()
            }

            override fun onBillingServiceDisconnected() {
                Toast.makeText(this@PremiumActivity, "You are disconnected from Billing", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Implement this method to get notifications for purchases updates. Both purchases initiated by
     * your app and the ones initiated outside of your app will be reported here.
     *
     *
     * **Warning!** All purchases reported here must either be consumed or acknowledged. Failure
     * to either consume (via [BillingClient.consumeAsync]) or acknowledge (via [ ][BillingClient.acknowledgePurchase]) a purchase will result in that purchase being refunded.
     * Please refer to
     * https://developer.android.com/google/play/billing/billing_library_overview#acknowledge for more
     * details.
     *
     * @param billingResult BillingResult of the update.
     * @param purchases List of updated purchases if present.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult?, purchases: MutableList<Purchase>?) {
        Toast.makeText(this, "Purchases item: " + purchases!!.size, Toast.LENGTH_SHORT).show()
    }
}
