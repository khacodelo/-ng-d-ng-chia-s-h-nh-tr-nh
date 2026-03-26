const mongoose = require("mongoose");

const JourneySchema = new mongoose.Schema({
    userId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: "User",
        required: true
    },
    startTime: {
        type: Date,
        default: Date.now
    },
    endTime: {
        type: Date
    },
    distance: {
        type: Number, // In meters
        default: 0
    },
    points: [{
        lat: Number,
        lng: Number,
        timestamp: { type: Date, default: Date.now }
    }]
});

module.exports = mongoose.model("Journey", JourneySchema);
