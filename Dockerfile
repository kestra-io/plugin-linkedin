# Dockerfile
FROM kestra/kestra:latest

# Copy the built plugin
COPY --chown=kestra:kestra build/libs/*.jar /app/plugins/

# Set environment variables
ENV KESTRA_PLUGINS_PATH=/app/plugins
ENV MICRONAUT_ENVIRONMENTS=local

# Expose port
EXPOSE 8080

# Run as the non-root user defined by the base image
USER kestra

# Start Kestra
CMD ["server", "local"]