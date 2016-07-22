package avalanche.core.channel;

import android.support.annotation.NonNull;

import avalanche.core.ingestion.models.Log;

/**
 * The interface for AvalancheChannel
 */
public interface AvalancheChannel {

    /**
     * Add Log to queue to be persisted and sent.
     *
     * @param log       the Log to be enqueued
     * @param queueName the queue to use
     */
    void enqueue(@NonNull Log log, @NonNull @GroupNameDef String queueName);

    /**
     * Check whether channel is enabled or disabled.
     *
     * @return true if channel is enabled, false otherwise.
     */
    boolean isEnabled();

    /**
     * Enable or disable channel.
     *
     * @param enabled true to enable, false to disable.
     */
    void setEnabled(boolean enabled);
}