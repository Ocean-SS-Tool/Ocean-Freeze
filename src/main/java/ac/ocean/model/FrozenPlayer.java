package ac.ocean.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.Location;

import java.util.UUID;

@Getter
@Setter
public class FrozenPlayer {

    private final UUID playerUuid;
    private final UUID freezerUuid;
    private final Location freezeLocation;
    private final GameMode originalGameMode;
    private final long freezeTime;
    private String scanPin;
    private boolean scanStarted;
    private int messageTaskId;
    private int scanInstructionsTaskId;
    private int scanMonitoringTaskId;
    private boolean scanFinished;

    public FrozenPlayer(UUID playerUuid, UUID freezerUuid, Location freezeLocation,
                        GameMode originalGameMode, long freezeTime) {
        this.playerUuid = playerUuid;
        this.freezerUuid = freezerUuid;
        this.freezeLocation = freezeLocation.clone();
        this.originalGameMode = originalGameMode;
        this.freezeTime = freezeTime;
        this.scanStarted = false;
        this.messageTaskId = -1;
        this.scanInstructionsTaskId = -1;
        this.scanMonitoringTaskId = -1;
        this.scanFinished = false;
    }
}
