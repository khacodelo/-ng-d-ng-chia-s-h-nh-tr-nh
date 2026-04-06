const express = require("express");
const router = express.Router();
const Journey = require("../models/Journey");
const jwt = require("jsonwebtoken");
const multer = require("multer");
const cloudinary = require("cloudinary").v2;
const streamifier = require("streamifier");

// KHÓA CỨNG THÔNG SỐ ĐỂ FIX TRIỆT ĐỂ LỖI UNKNOWN API KEY
cloudinary.config({
    cloud_name: "dmrtxveay",
    api_key: "879439655829881",
    api_secret: "6r2o63D5Yl817mY-A-L1v1T7C_k"
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

// API UPLOAD ẢNH
router.post("/upload", upload.single("image"), async (req, res) => {
    console.log("-----------------------------------------");
    console.log("Đang tải ảnh lên Cloudinary...");

    if (!req.file) {
        return res.status(400).json({ message: "No file uploaded." });
    }

    const stream = cloudinary.uploader.upload_stream(
        { folder: "app_geo_journeys" },
        (error, result) => {
            if (error) {
                console.error("LỖI TỪ CLOUDINARY:", error.message);
                return res.status(500).json({ message: "Lỗi Cloudinary: " + error.message });
            }
            console.log("TẢI ẢNH THÀNH CÔNG! URL:", result.secure_url);
            res.json({ imageUrl: result.secure_url });
        }
    );

    streamifier.createReadStream(req.file.buffer).pipe(stream);
});

router.get("/all", async (req, res) => {
    try {
        const journeys = await Journey.find()
            .populate("userId", "email")
            .sort({ startTime: -1 });
        res.json(journeys);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
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
