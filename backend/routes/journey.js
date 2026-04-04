const express = require("express");
const router = express.Router();
const Journey = require("../models/Journey");
const jwt = require("jsonwebtoken");
const multer = require("multer");
const cloudinary = require("cloudinary").v2;
const streamifier = require("streamifier");

cloudinary.config({
    cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
    api_key: process.env.CLOUDINARY_API_KEY,
    api_secret: process.env.CLOUDINARY_API_SECRET
});

const upload = multer();

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

// API: Lấy tất cả hành trình (Feed mạng xã hội)
router.get("/all", async (req, res) => {
    try {
        const journeys = await Journey.find()
            .populate("userId", "email") // Lấy thêm email người tạo
            .sort({ startTime: -1 }); // Mới nhất lên đầu
        res.json(journeys);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

router.post("/upload", upload.single("image"), async (req, res) => {
    if (!req.file) return res.status(400).send("No file uploaded.");
    const stream = cloudinary.uploader.upload_stream(
        { folder: "journeys" },
        (error, result) => {
            if (result) res.json({ imageUrl: result.secure_url });
            else res.status(500).json({ message: error.message });
        }
    );
    streamifier.createReadStream(req.file.buffer).pipe(stream);
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
