/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

public class RemoteControlRegistrar {
    private static final String TAG = "msb-rcr";
    private static Method mRegisterMediaButtonEventReceiver;
    private static Method mUnregisterMediaButtonEventReceiver;
    private AudioManager mAudioManager;
    private ComponentName mRemoteControlResponder;

    static {
        initializeRemoteControlRegistrationMethods();
    }

    public RemoteControlRegistrar(Context context, Class<?> receiverClass) {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mRemoteControlResponder = new ComponentName(context.getPackageName(),
                receiverClass.getName());
    }

    public void registerRemoteControl() {
        try {
            if (mRegisterMediaButtonEventReceiver == null) {
                return;
            }
            mRegisterMediaButtonEventReceiver.invoke(mAudioManager, mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e(TAG, "unexpected " + ie);
        }
    }

    public void unregisterRemoteControl() {
        try {
            if (mUnregisterMediaButtonEventReceiver == null) {
                return;
            }
            mUnregisterMediaButtonEventReceiver.invoke(mAudioManager, mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            System.err.println("unexpected " + ie);
        }
    }

    private static void initializeRemoteControlRegistrationMethods() {
        try {
            if (mRegisterMediaButtonEventReceiver == null) {
                mRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                        "registerMediaButtonEventReceiver", new Class[] {
                            ComponentName.class
                        });
            }
            if (mUnregisterMediaButtonEventReceiver == null) {
                mUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                        "unregisterMediaButtonEventReceiver", new Class[] {
                            ComponentName.class
                        });
            }
            /* success, this device will take advantage of better remote */
            /* control event handling */
        } catch (NoSuchMethodException nsme) {
            /* failure, still using the legacy behavior, but this app */
            /* is future-proof! */
        }
    }
}
