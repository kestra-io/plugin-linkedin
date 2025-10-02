# Dockerfile
FROM kestra/kestra:latest

# Copy the built plugin
COPY build/libs/*.jar /app/plugins/

# Set environment variables
ENV KESTRA_PLUGINS_PATH=/app/plugins
ENV MICRONAUT_ENVIRONMENTS=local

# Expose port
EXPOSE 8080

# Start Kestra
CMD ["server", "local"]