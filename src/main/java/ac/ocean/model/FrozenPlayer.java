package ac.ocean.model;

import org.bukkit.GameMode;
import org.bukkit.Location;

import java.util.UUID;

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

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public UUID getFreezerUuid() {
        return freezerUuid;
    }

    public Location getFreezeLocation() {
        return freezeLocation;
    }

    public GameMode getOriginalGameMode() {
        return originalGameMode;
    }

    public long getFreezeTime() {
        return freezeTime;
    }

    public String getScanPin() {
        return scanPin;
    }

    public void setScanPin(String scanPin) {
        this.scanPin = scanPin;
    }

    public boolean isScanStarted() {
        return scanStarted;
    }

    public void setScanStarted(boolean scanStarted) {
        this.scanStarted = scanStarted;
    }

    public int getMessageTaskId() {
        return messageTaskId;
    }

    public void setMessageTaskId(int messageTaskId) {
        this.messageTaskId = messageTaskId;
    }

    public int getScanInstructionsTaskId() {
        return scanInstructionsTaskId;
    }

    public void setScanInstructionsTaskId(int scanInstructionsTaskId) {
        this.scanInstructionsTaskId = scanInstructionsTaskId;
    }

    public int getScanMonitoringTaskId() {
        return scanMonitoringTaskId;
    }

    public void setScanMonitoringTaskId(int scanMonitoringTaskId) {
        this.scanMonitoringTaskId = scanMonitoringTaskId;
    }

    public boolean isScanFinished() {
        return scanFinished;
    }

    public void setScanFinished(boolean scanFinished) {
        this.scanFinished = scanFinished;
    }
}
