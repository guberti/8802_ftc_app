package org.firstinspires.ftc.teamcode.robot.mecanum.teleop;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.autonomous.waypoints.DelayedSubroutine;
import org.firstinspires.ftc.teamcode.autonomous.waypoints.Subroutines;
import org.firstinspires.ftc.teamcode.common.SimulatableMecanumOpMode;
import org.firstinspires.ftc.teamcode.robot.mecanum.SkystoneHardware;
import org.firstinspires.ftc.teamcode.robot.mecanum.MecanumPowers;
import org.firstinspires.ftc.teamcode.robot.mecanum.MecanumUtil;
import org.openftc.revextensions2.RevBulkData;


@Config
public abstract class SkystoneTeleop extends SimulatableMecanumOpMode {
    public static double TRIGGER_THRESHOLD = 0.2;

    SkystoneHardware robot;

    boolean leftStickButtonPrev, rightStickButtonPrev, rightTriggerPrev, leftBumperPrev, rightBumperPrev, aPrev, yPrev;


    boolean rightTriggerDropsBlock; // Toggles from grabbing to depositing
    boolean intakeOn;

    // Adjustable properties
    public abstract boolean fieldCentric();

    @Override
    public void init() {
        this.robot = this.getRobot();
    }

    @Override
    public void start() {
        robot.initBulkReadTelemetry();

        leftStickButtonPrev = gamepad1.left_stick_button;
        rightStickButtonPrev = gamepad1.right_stick_button;
        leftBumperPrev = gamepad1.left_bumper;
        rightBumperPrev = gamepad1.right_bumper;
        aPrev = gamepad1.a;
        yPrev = gamepad1.y;

        intakeOn = false;
        rightTriggerDropsBlock = false;
    }

    @Override
    public void loop() {
        RevBulkData data = robot.performBulkRead();
        robot.packet.put("layer", robot.pidLift.layer);
        robot.packet.put("targetPosition", robot.pidLift.targetPosition);
        robot.sendDashboardTelemetryPacket();

        /* Drive code */
        double slowScale = ((1 - gamepad1.left_trigger) * 0.7 + 0.3);
        double leftX = MecanumUtil.deadZone(-gamepad1.left_stick_x, 0.05) * slowScale;
        double leftY = MecanumUtil.deadZone(-gamepad1.left_stick_y, 0.05) * slowScale;
        double angle = -Math.atan2(leftY, leftX) + Math.PI / 2;
        if (fieldCentric()) {
            angle -= robot.pose().heading;
        }

        double driveScale = Math.sqrt(Math.pow(leftX, 2) + Math.pow(leftY, 2));
        driveScale = Range.clip(driveScale, 0, 1);

        // Exponentiate our turn
        double turn = Math.copySign(
                Math.pow(MecanumUtil.deadZone(-gamepad1.right_stick_x, 0.05), 2),
                -gamepad1.right_stick_x) * slowScale;

        MecanumPowers powers = MecanumUtil.powersFromAngle(angle, driveScale, turn);
        robot.setPowers(powers);

        /* Control intake */

        if (gamepad1.left_stick_button && !leftStickButtonPrev) {
            leftStickButtonPrev = true;
            intakeOn = !intakeOn; // Toggle intake
            robot.setIntakePower(intakeOn ? 1 : 0);
        } else if (!gamepad1.left_stick_button) {
            leftStickButtonPrev = false;
        }

        /* Block grabber */
        if (gamepad1.right_stick_button && !rightStickButtonPrev) {
            rightStickButtonPrev = true;
            robot.blockFlipper.readyBlockGrab();
            robot.blockGrabber.extend(); // Grab the block
            robot.actionCache.add(new DelayedSubroutine(600, Subroutines.SET_FLIPPER_DRIVING));
            rightTriggerDropsBlock = false;
        } else if (!gamepad1.right_stick_button) {
            rightStickButtonPrev = false;
        }

        /* Intake flipper */
        boolean leftTrigger = gamepad1.left_trigger > TRIGGER_THRESHOLD;
        boolean rightTrigger = gamepad1.right_trigger > TRIGGER_THRESHOLD;
        if (leftTrigger && !rightTrigger) {
            robot.blockFlipper.readyBlockIntake();
            rightTriggerDropsBlock = false;
        }

        if (rightTrigger && !rightTriggerPrev) {
            rightTriggerPrev = true;
            if (rightTriggerDropsBlock) {
                robot.blockGrabber.retract();
                robot.actionCache.add(new DelayedSubroutine(250, Subroutines.LIFT_A_LITTLE));
                robot.actionCache.add(new DelayedSubroutine(1000, Subroutines.SET_FLIPPER_INTAKING));
                robot.actionCache.add(new DelayedSubroutine(1000, Subroutines.LIFT_TO_ZERO));
            } else {
                robot.blockFlipper.normExtend();
            }
            rightTriggerDropsBlock = !rightTriggerDropsBlock;
            rightTriggerPrev = true;

            robot.blockFlipper.normExtend();
        } else if (!rightTrigger) {
            rightTriggerPrev = false;
        }

        /* Lit control */
        if (gamepad1.left_bumper && !leftBumperPrev) {
            leftBumperPrev = true;
            robot.pidLift.changeLayer(-1);
        } else if (!gamepad1.left_bumper) {
            leftBumperPrev = false;
        }

        if (gamepad1.right_bumper && !rightBumperPrev) {
            rightBumperPrev = true;
            robot.pidLift.changeLayer(1);
        } else if (!gamepad1.right_bumper) {
            rightBumperPrev = false;
        }

        /* Misc servos */
        if (gamepad1.y && !yPrev) {
            yPrev = true;
            robot.leftFoundationLatch.toggle();
            robot.rightFoundationLatch.toggle();
        } else if (!gamepad1.y) {
            yPrev = false;
        }

        if (gamepad1.a && !aPrev) {
            aPrev = true;
            robot.setIntakePower(-1);
        } else if (!gamepad1.a) {
            aPrev = false;
        }
    }
}
