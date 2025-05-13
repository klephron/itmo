# Defense

## Slide 1: Introduction

Good evening, everyone. My name is Nikita Kulakov, and I’m excited to present my project, the "Road Condition Monitoring System." Let’s dive into how this system addresses critical issues and how it offers its innovative solution.

## Slide 2: The Problem

Road quality is a pressing concern today, impacting driver safety and economic efficiency. Research shows that poor road conditions increase vehicle maintenance costs and travel times. Additionally, delayed repairs inflate servicing expenses.

To address road defects, traditional methods rely on specialized teams or mobile labs equipped with advanced tools like ultrasonic sensors. While accurate, these approaches are time-consuming and costly. An alternative is leveraging drivers’ mobile devices to collect data, offering a simpler, scalable solution for rapid analysis over large areas. This is the core idea behind my system.

## Slide 3: The Solution

Here’s a glimpse of the mobile application at the heart of the system. It visualizes road conditions on a map, using a color gradient: red for poor roads and green for good ones. With it drivers can easily assess road quality results. The app also includes sliders to filter data by date or highlight problematic roads.

I tested the app in real-world conditions in St. Petersburg, and the results have been promising.

## Slide 4: Mobile Application Features

Beyond visualization, the app enables data collection. Users can activate sensors and choose to send data  with a simple toggle—otherwise. While not sent data is stored locally. The interface also displays real-time sensor graphs, configuration settings, and a log page for troubleshooting, which is especially useful as the system is still under development.

## Slide 5: System Architecture

This slide outlines the system’s architecture. The mobile app handles data collection and visualization, supported by three key services: a user request service (named API), a forecasting service, and a results processing service.

I’ll keep this brief for now, but I’d be happy to dive deeper during the Q&A.

## Slide 6: System Testing

Let’s talk about performance. This slide demonstrates how the system handles high user loads. I developed a service to simulate multiple mobile devices and conducted a stress test. The results? In 30 seconds required to send data from mobile devices, the system processed 4,200 sensor data messages, with a total processing time of 45 seconds and generated 34,000 reports.

Thanks to the system’s horizontally scalable architecture, these results highlight its robustness and efficiency.

## Slide 7: Algorithm Testing

I evaluated several regression algorithms to optimize performance, including deterministic models, regression trees, and XGBoost, both with and without a machine speed feature. The slide compares their processing times and accuracy at each stage.

Ultimately, XGBoost models were selected for their balance of speed and low error rates when compared to true data.

## Slide 8: Algorithm Test Visualizations

This slide visualizes the test results from the previous slide. While XGBoost with the speed parameter performed better in some cases, it was less effective in real-world scenarios and less sensitive to certain road defects. As a result, I opted for a more reliable configuration.

## Slide 9: Conclusion

To wrap up, today I’ve covered:

- An analysis of the road quality problem and its significance.
- The core features of the mobile application for drivers.
- The system’s scalable architecture.
- The algorithms tested and their performance.
- And Real-world testing results demonstrating practical application.

## Slide 10: Closing

Thank you for your attention! I’m happy to answer any questions or discuss the project further.
