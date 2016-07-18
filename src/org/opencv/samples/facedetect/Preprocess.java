package org.opencv.samples.facedetect;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class Preprocess {

	public static Mat main(Mat image) {

		// 调整为灰度图
		image = GrayImageConvert(image);

		// 直方图均衡化
		image = GrayImageEqualization(image);

		// 优化，待选择

		// 输出
		return image;

	}

	// 输入图片，输出该图片直方图均衡化后的灰度图
	public static Mat GrayImageConvert(Mat image) {

		Mat image2 = new Mat();
		Imgproc.cvtColor(image, image2, Imgproc.COLOR_BGR2GRAY);
		image = image2;

		return image;
	}

	public static Mat GrayImageEqualization(Mat image) {

		Mat image2 = new Mat();
		Imgproc.equalizeHist(image, image2);
		image = image2;

		return image;
	}
}
