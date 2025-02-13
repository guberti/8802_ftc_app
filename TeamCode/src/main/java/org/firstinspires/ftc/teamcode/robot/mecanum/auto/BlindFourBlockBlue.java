package org.firstinspires.ftc.teamcode.robot.mecanum.auto;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.common.elements.Alliance;

@Autonomous(name="Blue five block", group="5Block")
public class BlindFourBlockBlue extends SSAutoMovingFoundation {
    @Override
    public void init() {
        this.ALLIANCE = Alliance.BLUE;
        super.init();
    }
}
