package com.arcsinw.nesemulator.utils;

import com.github.strikerx3.jxinput.*;
import com.github.strikerx3.jxinput.enums.XInputButton;
import com.github.strikerx3.jxinput.exceptions.XInputNotLoadedException;
import com.github.strikerx3.jxinput.listener.SimpleXInputDeviceListener;

import javax.swing.*;
import java.util.ArrayList;
import java.util.EventListener;

/**
 * Xbox手柄输入支持
 * https://github.com/StrikerX3/JXInput
 */
public class XboxController {
    private XInputDevice device = null;

    /**
     * Retrieve all devices
     * @return
     */
    public static XInputDevice[] getAllDevices() throws XInputNotLoadedException {
        return XInputDevice.getAllDevices();
    }

    // region 按键事件

    public interface ButtonStateChangedEventListener extends EventListener {
        /**
         * 手柄按键状态变化时触发
         * @param button 状态变化的按键
         * @param pressed 按键当前状态 true-按下 false-松开
         */
        void notifyButtonStateChanged(final XInputButton button, final boolean pressed);
    }

    private final ArrayList<ButtonStateChangedEventListener> listeners = new ArrayList<>();

    public void addListener(ButtonStateChangedEventListener l) {
        listeners.add(l);
    }

    public void notifyButtonStateChanged(final XInputButton button, final boolean pressed) {
        for(ButtonStateChangedEventListener l : listeners) {
            l.notifyButtonStateChanged(button, pressed);
        }
    }

    // endregion

    public XboxController() {
        try {
            device = XInputDevice.getDeviceFor(0);
            if (device != null) {
                device.addListener(new SimpleXInputDeviceListener(){
                    @Override
                    public void connected() {
                        System.out.println("Controller connected");
                    }

                    @Override
                    public void disconnected() {
                        System.out.println("Controller disconnected");
                    }

                    @Override
                    public void buttonChanged(final XInputButton button, final boolean pressed) {
                        notifyButtonStateChanged(button, pressed);
                    }
                });
            }
        } catch (XInputNotLoadedException e) {
            e.printStackTrace();
        }

        Timer timer = new Timer(10, args -> {
            if (device != null) {
                device.poll();
            }
        });

        timer.start();
    }

    /**
     * 使手柄震动
     * @param left 左边震动幅度 0 ~ 65535
     * @param right 右边震动幅度 0 ~ 65535
     * @param duration 震动时间（毫秒）
     */
    public void vibrate(int left, int right, long duration) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < duration) {
            device.setVibration(left, right);
        }
    }

    /**
     * 使手柄震动
     * @param left 左边震动幅度 0 ~ 65535
     * @param right 右边震动幅度 0 ~ 65535
     */
    public void vibrate(int left, int right) {
        vibrate(left, right, 1000);
    }


    public static void main(String[] args) {
//        while (true) {
//            // First we need to poll data.
//            // poll() will return false if the device is not connected
//            if (xboxController.device.poll()) {
//                // Retrieve the components
//                XInputComponents components = xboxController.device.getComponents();
//
//                XInputButtons buttons = components.getButtons();
//                XInputAxes axes = components.getAxes();
//
//                // Buttons and axes have public fields (although this is not idiomatic Java)
//
//
//                // Retrieve button state
//                if (buttons.a) {
//                    // The A button is currently pressed
//                    xboxController.vibrate(1000, 50000);
//                }
//                if (buttons.b) {
//                    xboxController.vibrate(0, 0);
//                }
//
//                if (buttons.up) {
//
//                }
//
//                // Check if Guide button is supported
//                if (XInputDevice.isGuideButtonSupported()) {
//                    // Use it
//                    if (buttons.guide) {
//                        // The Guide button is currently pressed
//                    }
//                }
//
//                // Retrieve axis state
//                float acceleration = axes.rt;
//                float brake = axes.lt;
//            } else {
//                // Controller is not connected; display a message
//            }
//        }
    }

}
