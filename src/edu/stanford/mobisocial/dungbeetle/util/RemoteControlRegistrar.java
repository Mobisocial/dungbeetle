
package edu.stanford.mobisocial.dungbeetle.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import edu.stanford.mobisocial.dungbeetle.RemoteControlReceiver;

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

    public RemoteControlRegistrar(Context context) {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mRemoteControlResponder = new ComponentName(context.getPackageName(),
                RemoteControlReceiver.class.getName());
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
