package org.firstinspires.ftc.teamcode.robot.mecanum;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.simulator.SimulatedOpModeFactory;
import org.firstinspires.ftc.teamcode.common.math.Pose;
import org.firstinspires.ftc.teamcode.robot.mecanum.mechanisms.HorizontalSlide;
import org.firstinspires.ftc.teamcode.robot.mecanum.mechanisms.SimpleLift;
import org.firstinspires.ftc.teamcode.robot.mecanum.teleop.SkystoneTeleop;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkystoneTeleopFunctionsTest {

    private void verifyMockMotorPower(DcMotorEx m, double power) {
        Mockito.verify(m).setPower(power);
    }

    private static double FL_LEFT = SkystoneHardware.FOUNDATION_LATCH_OPEN + SkystoneHardware.FOUNDATION_LATCH_LR_OFFSET;
    private static double FL_RIGHT = SkystoneHardware.FOUNDATION_LATCH_OPEN - SkystoneHardware.FOUNDATION_LATCH_LR_OFFSET;

    @Test
    void opModeTest() {
        SimulatedOpModeFactory simOpMode = new SimulatedOpModeFactory(SkystoneTeleop.class);
        simOpMode.opMode.start();

        testStartingPosition(simOpMode, simOpMode.robot);
        testToggleServos(simOpMode, simOpMode.robot);
        simulateCycle(simOpMode, simOpMode.robot);
    }

    private void testStartingPosition(SimulatedOpModeFactory simOpMode, SkystoneHardware robot) {
        // At start, our position should be 0, 0 after waiting a bit
        simOpMode.elapseCycles(10, 0.1);
        assertEquals(simOpMode.robot.pose(), new Pose(0, 0, 0));
        assertEquals(simOpMode.robot.wheelPowers, new MecanumPowers(0,0, 0, 0));
        assertEquals(simOpMode.robot.wheelPowers, new MecanumPowers(0,0, 0, 0));

        // All our mechanisms should be in their default states
        assertEquals(DcMotor.RunMode.RUN_TO_POSITION, robot.liftLeft.getMode());
        assertEquals(0, robot.liftLeft.getTargetPosition());
        assertTrue(robot.liftLeft.getPower() > 0);

        assertMotorOff(robot.intakeLeft);
        assertMotorOff(robot.intakeRight);

        // Ensure all servos are going to starting place
        assertEquals(HorizontalSlide.LEFT_INTAKING, robot.blockFlipper.leftFlipper.getPosition());
        assertEquals(HorizontalSlide.RIGHT_INTAKING, robot.blockFlipper.rightFlipper.getPosition());
        assertNotEquals(robot.blockFlipper.leftFlipper.getDirection(), robot.blockFlipper.rightFlipper.getDirection());
        assertEquals(FL_LEFT, robot.leftFoundationLatch.servo.getPosition());
        assertEquals(FL_RIGHT, robot.rightFoundationLatch.servo.getPosition());
        assertNotEquals(robot.leftFoundationLatch.servo.getDirection(), robot.rightFoundationLatch.servo.getDirection());
        assertEquals(SkystoneHardware.BLOCK_GRABBER_OPEN, robot.blockGrabber.servo.getPosition());
    }

    private void testToggleServos(SimulatedOpModeFactory simOpMode, SkystoneHardware robot) {
        /* Test foundation latch toggle */
        System.out.println(robot.leftFoundationLatch.servo.getPosition());
        assertEquals(FL_LEFT, robot.leftFoundationLatch.servo.getPosition());
        assertEquals(FL_RIGHT, robot.rightFoundationLatch.servo.getPosition());
        simOpMode.cycle();
        simOpMode.opMode.gamepad1.y = true;
        simOpMode.cycle();
        simOpMode.opMode.gamepad1.y = false;
        simOpMode.cycle();
        assertEquals(SkystoneHardware.FOUNDATION_LATCH_CLOSED + SkystoneHardware.FOUNDATION_LATCH_LR_OFFSET, robot.leftFoundationLatch.servo.getPosition());
        assertEquals(SkystoneHardware.FOUNDATION_LATCH_CLOSED - SkystoneHardware.FOUNDATION_LATCH_LR_OFFSET, robot.rightFoundationLatch.servo.getPosition());
        simOpMode.opMode.gamepad1.y = true;
        simOpMode.cycle();
        simOpMode.opMode.gamepad1.y = false;
        simOpMode.cycle();
        assertEquals(FL_LEFT, robot.leftFoundationLatch.servo.getPosition());
        assertEquals(FL_RIGHT, robot.rightFoundationLatch.servo.getPosition());
    }

    private void simulateCycle(SimulatedOpModeFactory simOpMode, SkystoneHardware robot) {
        // Verify we're intaking right now
        assertMotorOff(robot.intakeLeft);
        assertMotorOff(robot.intakeRight);

        simOpMode.opMode.gamepad1.left_stick_button = true;
        simOpMode.cycle();
        simOpMode.opMode.gamepad1.left_stick_button = false;
        simOpMode.cycle();

        assertEquals(1, robot.intakeLeft.getPower());
        assertEquals(1, robot.intakeRight.getPower());
        simOpMode.elapseCycles(2, 100);

        // Now simulate block pickup
        simOpMode.robot.dataGen.analogInputs[SkystoneHardware.TRAY_DETECTOR_PORT] = 10000;
        simOpMode.cycle();

        // Intake should automatically turn off
        assertMotorOff(robot.intakeLeft);
        assertMotorOff(robot.intakeRight);
        assertEquals(HorizontalSlide.LEFT_INTAKING, robot.blockFlipper.leftFlipper.getPosition());
        assertEquals(HorizontalSlide.RIGHT_INTAKING, robot.blockFlipper.rightFlipper.getPosition());

        // Now we grab the block
        simOpMode.opMode.gamepad1.right_trigger = 1;
        simOpMode.cycle();
        simOpMode.opMode.gamepad1.right_trigger = 0;
        simOpMode.cycle();

        // We should be trying to grab the block
        assertEquals(HorizontalSlide.LEFT_GRABBING, robot.blockFlipper.leftFlipper.getPosition());
        assertEquals(HorizontalSlide.RIGHT_GRABBING, robot.blockFlipper.rightFlipper.getPosition());

        simOpMode.elapseCycles(2, 100);
        assertEquals(HorizontalSlide.LEFT_DRIVING, robot.blockFlipper.leftFlipper.getPosition());
        assertEquals(HorizontalSlide.RIGHT_DRIVING, robot.blockFlipper.rightFlipper.getPosition());
        assertEquals(SkystoneHardware.BLOCK_GRABBER_CLOSED, robot.blockGrabber.servo.getPosition());

        simOpMode.opMode.gamepad1.right_trigger = 1;
        simOpMode.cycle();
        simOpMode.opMode.gamepad1.right_trigger = 0;
        simOpMode.cycle();

        assertEquals(HorizontalSlide.LEFT_NORM_EXTEND, robot.blockFlipper.leftFlipper.getPosition());
        assertEquals(HorizontalSlide.RIGHT_NORM_EXTEND, robot.blockFlipper.rightFlipper.getPosition());
        assertEquals(SkystoneHardware.BLOCK_GRABBER_CLOSED, robot.blockGrabber.servo.getPosition());
        assertEquals(0, robot.liftLeft.getTargetPosition());

        // Move lift up two levels
        /*simOpMode.opMode.gamepad1.dpad_right = true;
        simOpMode.cycle();
        simOpMode.opMode.gamepad1.dpad_right = false;
        simOpMode.cycle();
        simOpMode.opMode.gamepad1.dpad_right = true;
        simOpMode.cycle();
        simOpMode.opMode.gamepad1.dpad_right = false;
        simOpMode.cycle();
        assertEquals(SimpleLift.LAYER_SHIFT * 2, robot.liftLeft.getTargetPosition());

        // Place block
        simOpMode.opMode.gamepad1.right_trigger = 1;
        simOpMode.cycle();
        simOpMode.opMode.gamepad1.right_trigger = 0;
        simOpMode.cycle();

        // Ensure lift is moving up, but we're not retracting the servos yet
        assertTrue(SimpleLift.LAYER_SHIFT * 2 < robot.liftLeft.getTargetPosition());
        assertEquals(SkystoneHardware.BLOCK_GRABBER_OPEN, robot.blockGrabber.servo.getPosition());
        assertEquals(HorizontalSlide.LEFT_NORM_EXTEND, robot.blockFlipper.leftFlipper.getPosition());
        assertEquals(HorizontalSlide.RIGHT_NORM_EXTEND, robot.blockFlipper.rightFlipper.getPosition());

        // Now once sequence is over, we should be properly reset
        simOpMode.elapseCycles(2, 100);
        assertEquals(SkystoneHardware.BLOCK_GRABBER_OPEN, robot.blockGrabber.servo.getPosition());
        assertEquals(HorizontalSlide.LEFT_INTAKING, robot.blockFlipper.leftFlipper.getPosition());
        assertEquals(HorizontalSlide.RIGHT_INTAKING, robot.blockFlipper.rightFlipper.getPosition());
        assertEquals(1, robot.intakeLeft.getPower());
        assertEquals(1, robot.intakeRight.getPower());*/
    }

    private void assertMotorOff(DcMotorEx m) {
        assertEquals(0, m.getPower());
    }

}
