require("dotenv").config();
const express = require("express");
const cors = require("cors");
const path = require("path");
const connectDB = require("./config/db");
const fs = require("fs");

const app = express();

// 1. Tạo thư mục uploads nếu chưa có
const uploadDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadDir)){
    fs.mkdirSync(uploadDir);
}

// 2. Cấu hình Middleware
app.use(cors());
app.use(express.json({ limit: "50mb" }));
app.use(express.urlencoded({ limit: "50mb", extended: true }));
app.use("/uploads", express.static(uploadDir));

// 3. Kết nối Database
connectDB().then(() => {
    console.log("MongoDB connected thành công!");
}).catch(err => {
    console.error("Database connection failed:", err);
});

// 4. Cấu hình Routes
try {
    app.use("/auth", require("./routes/auth"));
    app.use("/journey", require("./routes/journey"));
    console.log("Routes đã được nạp.");
} catch (error) {
    console.error("Lỗi khi nạp Routes:", error.message);
}

app.get("/", (req, res) => {
    res.send("Server Hành Trình đang chạy ổn định!");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, "0.0.0.0", () => {
    console.log(`-----------------------------------------`);
    console.log(`SERVER ĐANG CHẠY TẠI CỔNG ${PORT}`);
    console.log(`-----------------------------------------`);
});

process.on('uncaughtException', (err) => {
    console.error('Lỗi hệ thống nghiêm trọng:', err.message);
});
