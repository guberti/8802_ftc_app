package org.firstinspires.ftc.teamcode.robot.mecanum.auto;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.autonomous.PurePursuitPath;
import org.firstinspires.ftc.teamcode.autonomous.waypoints.Waypoint;
import org.firstinspires.ftc.teamcode.common.SimulatableMecanumOpMode;
import org.firstinspires.ftc.teamcode.common.elements.Alliance;
import org.firstinspires.ftc.teamcode.common.elements.SkystoneState;
import org.firstinspires.ftc.teamcode.common.math.Pose;
import org.firstinspires.ftc.teamcode.robot.mecanum.MecanumUtil;
import org.firstinspires.ftc.teamcode.robot.mecanum.SkystoneHardware;
import org.firstinspires.ftc.teamcode.robot.mecanum.auto.vision.ImprovedSkystoneDetector;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;
import org.openftc.revextensions2.RevBulkData;

import java.util.List;

import static org.firstinspires.ftc.teamcode.robot.mecanum.SkystoneHardware.FIELD_RADIUS;

@Config
public abstract class PurePursuitAutoRed extends SimulatableMecanumOpMode {
    Pose DEFAULT_START_POSITION = new Pose(-FIELD_RADIUS + 22.75 + 9, -FIELD_RADIUS + 9, 1 * Math.PI / 2);

    SkystoneHardware robot;
    PurePursuitPath followPath;
    private Telemetry.Item skystoneSeen;

    // Robot state
    public static SkystoneState SKYSTONE = SkystoneState.UPPER;
    public static Alliance ALLIANCE = Alliance.RED;

    public abstract Pose getBlueStartPosition();
    public abstract List<Waypoint> getPurePursuitWaypoints();

    @Override
    public void init() {
        Pose start = getBlueStartPosition().clone();
        this.robot = this.getRobot(start);

        // During this we're also going to init our lift again
        robot.pidLift.left.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.pidLift.right.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        startPhoneCamDetector(ALLIANCE);
        robot.pidLift.left.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        robot.pidLift.right.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        // Display skystone on driver station
        skystoneSeen = telemetry.addLine().addData("Skystone", "LOADING");
    }

    @Override
    public void init_loop() {
        skystoneSeen.setValue(getSkystoneState().toString());
        telemetry.update();
    }

    @Override
    public void start() {
        telemetry.clearAll();
        robot.initBulkReadTelemetry();
        SKYSTONE = getSkystoneState();
        stopPhoneCamDetector();
        followPath = new PurePursuitPath(robot, getPurePursuitWaypoints());
        // We design all paths for blue side, and then flip them for red
        followPath.reverse();
    }

    @Override
    public void loop() {
        RevBulkData data = robot.performBulkRead();
        robot.drawDashboardPath(followPath);
        robot.sendDashboardTelemetryPacket();

        if (!followPath.finished()) {
            followPath.update();
        } else {
            robot.setPowers(MecanumUtil.STOP);
            stop();
        }
    }

    @Override
    public void stop() {
        robot.blockGrabber.retract();
    }
}
