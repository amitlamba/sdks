package com.userndot.androidsdk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.userndot.sdk.android.Event
import com.userndot.sdk.android.EventUser
import com.userndot.sdk.android.UserNDot

class MainActivity : AppCompatActivity() {

    private var defaultInstance: UserNDot? = null
    private var configInstance: UserNDot? = null
    private lateinit var event: Button
    private lateinit var profile: Button
    private lateinit var logout: Button
    private var eventWithProps: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        setSupportActionBar(toolbar)
        event = findViewById(R.id.button1)
        profile = findViewById(R.id.button2)
        logout=findViewById(R.id.button3)
//
        defaultInstance = UserNDot.getDefaultInstance(this);
//        defaultInstance?.findLocationOnUserNDotEnd()
        Log.e("UserNDot instance",defaultInstance.toString())

//        var config= UserNDotConfig.createInstance(this,"Account id","Token Id")

//        configInstance= UserNDot.getInstanceWithConfig(this,config = config)

        //record event
        (event).setOnClickListener {
            Log.e("event","clicked")
            var e = Event()
            e.city = "gurgaon"
            e.name = "Add to Cart"
            e.attributes= HashMap()
            e.attributes.put("Item","Shoes")
            e.attributes.put("new","new")
            e.country="India"
            e.state="Haryana"
            e.userIdentified=false
            defaultInstance?.pushEvent(e)
        }
        //Record an event with properties
//        eventWithProps?.setOnClickListener(View.OnClickListener {
//            val prodViewedAction = HashMap<String, Any>()
//            prodViewedAction["Product Name"] = "Casio Chronograph Watch"
//            prodViewedAction["Category"] = "Mens Accessories"
//            prodViewedAction["Price"] = 59.99
//            prodViewedAction["Date"] = java.util.Date()
//
//            defaultInstance?.pushEvent()
////            //OR
////            //cleverTapInstanceTwo.pushEvent("Product viewed", prodViewedAction);
//        });


        //Push a profile to CleverTap
        (profile).setOnClickListener {
            Log.e("profile","clicked")
            var user = EventUser()
            user.uid="user7"
            user.city="Gurugram"
            user.address=""
            user.country="India"
            user.countryCode="91"
            user.email="jogendertemp@gmail.com"
            user.dob="1993-01-01"
            user.firstName="Jogendra"
            user.gender="Male"
            user.lastName="Singh"
            user.mobile="1234567890"
            user.fbId="fb@facebook.com"
            user.googleId="google@google.com"
            defaultInstance?.onUserLogin(user)
        }

        (logout).setOnClickListener{
            Log.e("logout","clicked")
            defaultInstance?.onUserLogout()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // remove session id from shared preference
        //close executor
        //close database connection
    }
}
