const express = require("express");
const router = express.Router();
const Journey = require("../models/Journey");
const jwt = require("jsonwebtoken");
const multer = require("multer");
const path = require("path");

// Cấu hình lưu trữ ảnh cục bộ (Module 8 sẽ dùng Cloudinary)
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, "uploads/");
    },
    filename: (req, file, cb) => {
        cb(null, Date.now() + path.extname(file.originalname));
    }
});
const upload = multer({ storage: storage });

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

// API Upload ảnh riêng lẻ
router.post("/upload", upload.single("image"), (req, res) => {
    if (!req.file) return res.status(400).send("No file uploaded.");
    const imageUrl = `http://${req.hostname}:3000/uploads/${req.file.filename}`;
    res.json({ imageUrl });
});

router.post("/save", auth, async (req, res) => {
    const { startTime, endTime, distance, points, checkpoints } = req.body;
    try {
        const newJourney = new Journey({
            userId: req.user.id,
            startTime,
            endTime,
            distance,
            points,
            checkpoints
        });
        const savedJourney = await newJourney.save();
        res.json(savedJourney);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

module.exports = router;
