/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2017. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony.dataconnection;

import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.net.StringNetworkSpecifier;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import android.util.LocalLog;

import com.android.internal.telephony.PhoneSwitcher;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.SubscriptionMonitor;
import com.android.internal.util.IndentingPrintWriter;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class TelephonyNetworkFactory extends NetworkFactory {
    public final String LOG_TAG;
    protected static final boolean DBG = true;

    private final PhoneSwitcher mPhoneSwitcher;
    private final SubscriptionController mSubscriptionController;
    private final SubscriptionMonitor mSubscriptionMonitor;
    protected final DcTracker mDcTracker;

    protected final HashMap<NetworkRequest, LocalLog> mDefaultRequests =
            new HashMap<NetworkRequest, LocalLog>();
    protected final HashMap<NetworkRequest, LocalLog> mSpecificRequests =
            new HashMap<NetworkRequest, LocalLog>();

    private int mPhoneId;
    protected boolean mIsActive;
    protected boolean mIsDefault;
    private int mSubscriptionId;

    private final static int TELEPHONY_NETWORK_SCORE = 50;

    private final Handler mInternalHandler;
    private static final int EVENT_ACTIVE_PHONE_SWITCH          = 1;
    private static final int EVENT_SUBSCRIPTION_CHANGED         = 2;
    private static final int EVENT_DEFAULT_SUBSCRIPTION_CHANGED = 3;
    private static final int EVENT_NETWORK_REQUEST              = 4;
    private static final int EVENT_NETWORK_RELEASE              = 5;

    public TelephonyNetworkFactory(PhoneSwitcher phoneSwitcher,
            SubscriptionController subscriptionController, SubscriptionMonitor subscriptionMonitor,
            Looper looper, Context context, int phoneId, DcTracker dcTracker) {
        super(looper, context, "TelephonyNetworkFactory[" + phoneId + "]", null);
        mInternalHandler = new InternalHandler(looper);

        setCapabilityFilter(makeNetworkFilter(subscriptionController, phoneId));
        setScoreFilter(TELEPHONY_NETWORK_SCORE);

        mPhoneSwitcher = phoneSwitcher;
        mSubscriptionController = subscriptionController;
        mSubscriptionMonitor = subscriptionMonitor;
        mPhoneId = phoneId;
        LOG_TAG = "TelephonyNetworkFactory[" + phoneId + "]";
        mDcTracker = dcTracker;

        mIsActive = false;
        mPhoneSwitcher.registerForActivePhoneSwitch(mPhoneId, mInternalHandler,
                EVENT_ACTIVE_PHONE_SWITCH, null);

        mSubscriptionId = INVALID_SUBSCRIPTION_ID;
        mSubscriptionMonitor.registerForSubscriptionChanged(mPhoneId, mInternalHandler,
                EVENT_SUBSCRIPTION_CHANGED, null);

        mIsDefault = false;
        mSubscriptionMonitor.registerForDefaultDataSubscriptionChanged(mPhoneId, mInternalHandler,
                EVENT_DEFAULT_SUBSCRIPTION_CHANGED, null);

        register();
    }

    private NetworkCapabilities makeNetworkFilter(SubscriptionController subscriptionController,
            int phoneId) {
        final int subscriptionId = subscriptionController.getSubIdUsingPhoneId(phoneId);
        return makeNetworkFilter(subscriptionId);
    }

    protected NetworkCapabilities makeNetworkFilter(int subscriptionId) {
        NetworkCapabilities nc = new NetworkCapabilities();
        nc.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_SUPL);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_DUN);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_FOTA);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_IMS);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_CBS);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_IA);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_RCS);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_XCAP);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_EIMS);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        nc.setNetworkSpecifier(new StringNetworkSpecifier(String.valueOf(subscriptionId)));
        return nc;
    }

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_ACTIVE_PHONE_SWITCH: {
                    onActivePhoneSwitch();
                    break;
                }
                case EVENT_SUBSCRIPTION_CHANGED: {
                    onSubIdChange();
                    break;
                }
                case EVENT_DEFAULT_SUBSCRIPTION_CHANGED: {
                    onDefaultChange();
                    break;
                }
                case EVENT_NETWORK_REQUEST: {
                    onNeedNetworkFor(msg);
                    break;
                }
                case EVENT_NETWORK_RELEASE: {
                    onReleaseNetworkFor(msg);
                    break;
                }
            }
        }
    }

    protected static final int REQUEST_LOG_SIZE = 40;
    protected static final boolean REQUEST = true;
    private static final boolean RELEASE = false;

    protected void applyRequests(HashMap<NetworkRequest, LocalLog> requestMap, boolean action,
            String logStr) {
        for (NetworkRequest networkRequest : requestMap.keySet()) {
            if (ignoreCapabilityCheck(networkRequest.networkCapabilities)) {
                continue;
            }

            LocalLog localLog = requestMap.get(networkRequest);
            localLog.log(logStr);
            if (action == REQUEST) {
                mDcTracker.requestNetwork(networkRequest, localLog);
            } else {
                mDcTracker.releaseNetwork(networkRequest, localLog);
            }
        }
    }

    // apply or revoke requests if our active-ness changes
    private void onActivePhoneSwitch() {
        final boolean newIsActive = mPhoneSwitcher.isPhoneActive(mPhoneId);
        if (mIsActive != newIsActive) {
            mIsActive = newIsActive;
            String logString = "onActivePhoneSwitch(" + mIsActive + ", " + mIsDefault + ")";
            if (DBG) log(logString);
            if (mIsDefault) {
                applyRequests(mDefaultRequests, (mIsActive ? REQUEST : RELEASE), logString);
            }
            applyRequests(mSpecificRequests, (mIsActive ? REQUEST : RELEASE), logString);
        }
    }

    // watch for phone->subId changes, reapply new filter and let
    // that flow through to apply/revoke of requests
    private void onSubIdChange() {
        final int newSubscriptionId = mSubscriptionController.getSubIdUsingPhoneId(mPhoneId);
        if (mSubscriptionId != newSubscriptionId) {
            if (DBG) log("onSubIdChange " + mSubscriptionId + "->" + newSubscriptionId);
            mSubscriptionId = newSubscriptionId;
            setCapabilityFilter(makeNetworkFilter(mSubscriptionId));
        }
    }

    // watch for default-data changes (could be side effect of
    // phoneId->subId map change or direct change of default subId)
    // and apply/revoke default-only requests.
    private void onDefaultChange() {
        final int newDefaultSubscriptionId = mSubscriptionController.getDefaultDataSubId();
        final boolean newIsDefault = (newDefaultSubscriptionId == mSubscriptionId);
        if (newIsDefault != mIsDefault) {
            mIsDefault = newIsDefault;
            String logString = "onDefaultChange(" + mIsActive + "," + mIsDefault + ")";
            if (DBG) log(logString);
            if (mIsActive == false) return;
            applyRequests(mDefaultRequests, (mIsDefault ? REQUEST : RELEASE), logString);
        }
    }

    @Override
    public void needNetworkFor(NetworkRequest networkRequest, int score) {
        Message msg = mInternalHandler.obtainMessage(EVENT_NETWORK_REQUEST);
        msg.obj = networkRequest;
        msg.sendToTarget();
    }

    protected void onNeedNetworkFor(Message msg) {
        NetworkRequest networkRequest = (NetworkRequest)msg.obj;
        boolean isApplicable = false;
        LocalLog localLog = null;
        if (networkRequest.networkCapabilities.getNetworkSpecifier() == null) {
            // request only for the default network
            localLog = mDefaultRequests.get(networkRequest);
            if (localLog == null) {
                localLog = new LocalLog(REQUEST_LOG_SIZE);
                localLog.log("created for " + networkRequest);
                mDefaultRequests.put(networkRequest, localLog);
                isApplicable = mIsDefault;
            }
        } else {
            localLog = mSpecificRequests.get(networkRequest);
            if (localLog == null) {
                localLog = new LocalLog(REQUEST_LOG_SIZE);
                mSpecificRequests.put(networkRequest, localLog);
                isApplicable = true;
            }
        }
        if (mIsActive && isApplicable
                || (ignoreCapabilityCheck(networkRequest.networkCapabilities))) {
            String s = "onNeedNetworkFor";
            localLog.log(s);
            log(s + " " + networkRequest);
            mDcTracker.requestNetwork(networkRequest, localLog);
        } else {
            String s = "not acting - isApp=" + isApplicable + ", isAct=" + mIsActive;
            localLog.log(s);
            log(s + " " + networkRequest);
        }
    }

    @Override
    public void releaseNetworkFor(NetworkRequest networkRequest) {
        Message msg = mInternalHandler.obtainMessage(EVENT_NETWORK_RELEASE);
        msg.obj = networkRequest;
        msg.sendToTarget();
    }

    protected void onReleaseNetworkFor(Message msg) {
        NetworkRequest networkRequest = (NetworkRequest)msg.obj;
        LocalLog localLog = null;
        boolean isApplicable = false;
        if (networkRequest.networkCapabilities.getNetworkSpecifier() == null) {
            // request only for the default network
            localLog = mDefaultRequests.remove(networkRequest);
            isApplicable = (localLog != null) && mIsDefault;
        } else {
            localLog = mSpecificRequests.remove(networkRequest);
            isApplicable = (localLog != null);
        }
        if (mIsActive && isApplicable
                || (ignoreCapabilityCheck(networkRequest.networkCapabilities))) {
            String s = "onReleaseNetworkFor";
            localLog.log(s);
            log(s + " " + networkRequest);
            mDcTracker.releaseNetwork(networkRequest, localLog);
        } else {
            String s = "not releasing - isApp=" + isApplicable + ", isAct=" + mIsActive;
            localLog.log(s);
            log(s + " " + networkRequest);
        }
    }

    protected void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        pw.println(LOG_TAG + " mSubId=" + mSubscriptionId + " mIsActive=" +
                mIsActive + " mIsDefault=" + mIsDefault);
        pw.println("Default Requests:");
        pw.increaseIndent();
        for (NetworkRequest nr : mDefaultRequests.keySet()) {
            pw.println(nr);
            pw.increaseIndent();
            mDefaultRequests.get(nr).dump(fd, pw, args);
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }

    /**
     * Anchor method  to execute the special request even if the TNF not active phone.
     * @param capabilities used to check the network should be controlled by mIsActive or not.
     * @return true if the network doesn't need to be controlled by mIsActive.
     */
    protected boolean ignoreCapabilityCheck(NetworkCapabilities capabilities) {
        return false;
    }
}
