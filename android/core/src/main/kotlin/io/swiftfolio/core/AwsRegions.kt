package io.swiftfolio.core

/** A commercial AWS region: its code and the place name shown next to it. */
data class AwsRegion(val code: String, val location: String)

/** Standard commercial AWS regions that host S3, in the console's order. */
object AwsRegions {
    /** All regions; the first one is the default. */
    val all: List<AwsRegion> = listOf(
        AwsRegion("us-east-1", "N. Virginia"),
        AwsRegion("us-east-2", "Ohio"),
        AwsRegion("us-west-1", "N. California"),
        AwsRegion("us-west-2", "Oregon"),
        AwsRegion("af-south-1", "Cape Town"),
        AwsRegion("ap-east-1", "Hong Kong"),
        AwsRegion("ap-south-1", "Mumbai"),
        AwsRegion("ap-south-2", "Hyderabad"),
        AwsRegion("ap-southeast-1", "Singapore"),
        AwsRegion("ap-southeast-2", "Sydney"),
        AwsRegion("ap-southeast-3", "Jakarta"),
        AwsRegion("ap-southeast-4", "Melbourne"),
        AwsRegion("ap-northeast-1", "Tokyo"),
        AwsRegion("ap-northeast-2", "Seoul"),
        AwsRegion("ap-northeast-3", "Osaka"),
        AwsRegion("ca-central-1", "Canada Central"),
        AwsRegion("ca-west-1", "Calgary"),
        AwsRegion("eu-central-1", "Frankfurt"),
        AwsRegion("eu-central-2", "Zurich"),
        AwsRegion("eu-west-1", "Ireland"),
        AwsRegion("eu-west-2", "London"),
        AwsRegion("eu-west-3", "Paris"),
        AwsRegion("eu-north-1", "Stockholm"),
        AwsRegion("eu-south-1", "Milan"),
        AwsRegion("eu-south-2", "Spain"),
        AwsRegion("il-central-1", "Tel Aviv"),
        AwsRegion("me-south-1", "Bahrain"),
        AwsRegion("me-central-1", "UAE"),
        AwsRegion("sa-east-1", "São Paulo"),
    )

    /** Region with [code], or null when unknown. */
    fun find(code: String): AwsRegion? = all.firstOrNull { it.code == code }

    /** Regions whose code or location contains [query], case-insensitive. */
    fun search(query: String): List<AwsRegion> {
        val needle = query.trim()
        if (needle.isEmpty()) return all
        return all.filter { it.code.contains(needle, ignoreCase = true) || it.location.contains(needle, ignoreCase = true) }
    }
}
