package com.bglobal.lib.webrtc.callback

import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver

open class PeerConnectionObserverImpl : PeerConnection.Observer {
    override fun onSignalingChange(signalingState: PeerConnection.SignalingState?) {}
    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {}
    override fun onIceConnectionReceivingChange(change: Boolean) {}
    override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState?) {}
    override fun onIceCandidate(iceCandidate: IceCandidate?) {}
    override fun onIceCandidatesRemoved(iceCandidateList: Array<out IceCandidate>?) {}
    override fun onAddStream(track: MediaStream?) {}
    override fun onRemoveStream(track: MediaStream?) {}
    override fun onDataChannel(dataChannel: DataChannel?) {}
    override fun onRenegotiationNeeded() {}
    override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreamList: Array<out MediaStream>?) {}
    override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) { super.onStandardizedIceConnectionChange(newState) }
    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) { super.onConnectionChange(newState) }
    override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) { super.onSelectedCandidatePairChanged(event) }
    override fun onTrack(transceiver: RtpTransceiver?) { super.onTrack(transceiver) }
}
