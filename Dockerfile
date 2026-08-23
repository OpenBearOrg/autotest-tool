FROM eclipse-temurin:21-jre
WORKDIR /opt/autotest-tool
COPY autotest-dist/target/autotest-tool-1.0.0.jar /opt/autotest-tool/autotest-tool.jar
RUN mkdir -p /opt/autotest-tool/lib/jdbc \
    && useradd --system --uid 10001 autotest
USER 10001
WORKDIR /workspace
ENTRYPOINT ["java", "-cp", "/opt/autotest-tool/autotest-tool.jar:/opt/autotest-tool/lib/jdbc/*", "org.openbear.tool.autotest.cli.AutotestCli"]
CMD ["--help"]
