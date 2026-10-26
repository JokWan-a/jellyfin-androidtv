export class NativeVideoPlayerPlugin {
    constructor({ playbackManager, loading }) {
        this.playbackManager = playbackManager;
        this.loading = loading;

        this.name = 'Native player';
        this.type = 'mediaplayer';
        this.id = 'nativevideoplayer';

        // Prioritize first
        this.priority = -1;
        this.isLocalPlayer = true;

        this._nativePlayer = window['NativePlayer'];
    }

    async play(options) {
        this._nativePlayer.loadPlayer(JSON.stringify({
            ids: options.items.map(item => item.Id),
            startIndex: options.startIndex ?? null,
            startPositionTicks: options.startPositionTicks ?? null
        }));
        this.loading.hide();
    }

    shuffle(item) {}

    instantMix(item) {}

    queue(options) {}

    queueNext(options) {}

    canPlayMediaType(mediaType) {
        return mediaType === 'Video';
    }

    canQueueMediaType(mediaType) {
        return this.canPlayMediaType(mediaType);
    }

    canPlayItem(item, playOptions) {
        return playOptions.fullscreen &&
            !this.playbackManager.syncPlayEnabled;
    }

    // jellyfin-web never makes this its current player, so playback controls stay native.
    // Remote control commands reach the native player through its own websocket instead.
    async stop(destroyPlayer) {}

    nextTrack() {}

    previousTrack() {}

    seek(ticks) {}

    currentTime(ms) {
        return 0;
    }

    duration(val) {
        return null;
    }

    volume(volume) {
        return null;
    }

    getVolume() {}

    setVolume(vol) {}

    volumeUp() {}

    volumeDown() {}

    isMuted() {
        return false;
    }

    setMute(mute) {}

    toggleMute() {}

    paused() {
        return false;
    }

    pause() {}

    unpause() {}

    playPause() {}

    canSetAudioStreamIndex() {
        return false;
    }

    setAudioStreamIndex(index) {}

    setSubtitleStreamIndex(index) {}

    async changeAudioStream(index) {}

    async changeSubtitleStream(index) {}

    getPlaylist() {
        return Promise.resolve([]);
    }

    getCurrentPlaylistItemId() {}

    setCurrentPlaylistItem() {
        return Promise.resolve();
    }

    removeFromPlaylist() {
        return Promise.resolve();
    }

    destroy() {}

    async getDeviceProfile() {
        return {
            Name: 'Native Player Stub',
            MaxStreamingBitrate: 100000000,
            MaxStaticBitrate: 100000000,
            MusicStreamingTranscodingBitrate: 320000,
            DirectPlayProfiles: [{Type: 'Video'}, {Type: 'Audio'}],
            CodecProfiles: [],
            SubtitleProfiles: [],
            TranscodingProfiles: []
        };
    }
}
