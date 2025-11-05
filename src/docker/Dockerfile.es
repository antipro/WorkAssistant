FROM elasticsearch:8.19.6

# Set ES version variable so it's easy to change
# Install IK analyzer plugin (replace URL/version if you use a different ES version)
# The release asset URL pattern (example) :
RUN elasticsearch-plugin install --batch https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.19.6.zip

# Expose ports
EXPOSE 9200 9300

# Use the official entrypoint, nothing else to override