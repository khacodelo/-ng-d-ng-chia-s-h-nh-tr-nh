const express = require("express");
const router = express.Router();
const Journey = require("../models/Journey");
const jwt = require("jsonwebtoken");

// Middleware để kiểm tra Token
const auth = (req, res, next) => {
    const token = req.header("x-auth-token");
    if (!token) return res.status(401).json({ message: "No token, authorization denied" });

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.user = decoded;
        next();
    } catch (e) {
        res.status(400).json({ message: "Token is not valid" });
    }
};

// API: Lưu hành trình mới
router.post("/save", auth, async (req, res) => {
    const { startTime, endTime, distance, points } = req.body;

    try {
        const newJourney = new Journey({
            userId: req.user.id,
            startTime,
            endTime,
            distance,
            points
        });

        const savedJourney = await newJourney.save();
        res.json(savedJourney);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

module.exports = router;
