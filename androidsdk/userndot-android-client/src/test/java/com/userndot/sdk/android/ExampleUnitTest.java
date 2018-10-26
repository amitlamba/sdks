package com.userndot.sdk.android;

import org.junit.Test;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testDatabase(){
//        UserNDot obj=UserNDot.Companion.getDefaultInstance();
        HashMap<String,String> map=new HashMap<>();
        map.put("segmentid","6");
        map.put("value","4");
        Data data=new Data();
        data.setObjectData("object of eventuser");
        data.setType("eventuser");
        data.setTime(new Date().toString());


        com.userndot.sdk.android.Identity i=new com.userndot.sdk.android.Identity();

//        i.setSessionId("54");
//        i.setDeviceId("5545");
//        i.setClientId(4);
//        Event event=new Event();
//        EventUser user=new EventUser();
//        event.name="cart";
//        event.identity=identity;
//
//        user.setIdentity(identity);

//        String in=obj.sendAppLocalData("http://userndot.com/event/event/initialize","POST",null,null);
//        obj.sendAppLocalData("http://userndot.com/event/push/event","POST",null,event);
//        obj.sendAppLocalData("http://userndot.com/event/push/profile","POST",null,user);
    }
}
