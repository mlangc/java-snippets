package at.mlangc.more.log4j2.demo;

import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

class MoreLog4j2Demo {
	private static final Logger LOG = LoggerFactory.getLogger(MoreLog4j2Demo.class);

	static void main() {
		var loggers = Stream.of("consoleOnly", "dynatraceHttp", "dynatraceAsyncHttp")
				.map(suffix -> LoggerFactory.getLogger(MoreLog4j2Demo.class.getName() + "." + suffix))
				.toList();

		for (Logger logger : loggers) {
			var banner = "=".repeat(20 + logger.getName().length());
			LOG.info(banner);
			LOG.info("=========={}==========", logger.getName());

			var stopWatch = Stopwatch.createStarted();
			for (int i = 0; i < 10; i++) {
				logger.info("{}. One more log for king and country!", i + 1);
			}

			LOG.info("Done logging after {}", stopWatch.elapsed());
			LOG.info(banner);
		}

		// Log Monitoring API v2:
		// https://docs.dynatrace.com/docs/dynatrace-api/environment-api/log-monitoring-v2/post-ingest-logs
		var rawJsonLogger = LoggerFactory.getLogger(MoreLog4j2Demo.class.getName() + ".rawJson");
		rawJsonLogger.info("This is how the raw JSON that is sent to the Dynatrace API looks like");
	}
}
