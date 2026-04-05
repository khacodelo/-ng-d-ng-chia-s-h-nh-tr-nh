require("dotenv").config();
const express = require("express");
const cors = require("cors");
const path = require("path");
const connectDB = require("./config/db");
const fs = require("fs");

const app = express();

// Tạo thư mục uploads nếu chưa có
const uploadDir = "./uploads";
if (!fs.existsSync(uploadDir)){
    fs.mkdirSync(uploadDir);
}

app.use(cors());
app.use(express.json());

// Cho phép truy cập file ảnh từ trình duyệt/app
app.use("/uploads", express.static(path.join(__dirname, "uploads")));

// connect DB
connectDB();

// routes
app.use("/auth", require("./routes/auth"));
app.use("/journey", require("./routes/journey"));

const PORT = process.env.PORT || 3000;

// QUAN TRỌNG: Sửa listen thành "0.0.0.0" để cho phép máy thật kết nối
app.listen(PORT, "0.0.0.0", () => {
    console.log("Server running on port " + PORT);
});
