import re
import os

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Replacements for padding and spacing
    content = re.sub(r'\.padding\(16\.dp\)', '.padding(GymCoachSpacing.lg)', content)
    content = re.sub(r'\.padding\(8\.dp\)', '.padding(GymCoachSpacing.sm)', content)
    content = re.sub(r'\.padding\(12\.dp\)', '.padding(GymCoachSpacing.md)', content)
    content = re.sub(r'\.padding\(24\.dp\)', '.padding(GymCoachSpacing.xxl)', content)
    content = re.sub(r'Arrangement\.spacedBy\(8\.dp\)', 'Arrangement.spacedBy(GymCoachSpacing.sm)', content)
    content = re.sub(r'Arrangement\.spacedBy\(12\.dp\)', 'Arrangement.spacedBy(GymCoachSpacing.md)', content)
    content = re.sub(r'Arrangement\.spacedBy\(16\.dp\)', 'Arrangement.spacedBy(GymCoachSpacing.lg)', content)

    # Shapes
    content = re.sub(r'RoundedCornerShape\(16\.dp\)', 'GymCoachShapes.lg', content)
    content = re.sub(r'RoundedCornerShape\(12\.dp\)', 'GymCoachShapes.md', content)
    content = re.sub(r'RoundedCornerShape\(8\.dp\)', 'GymCoachShapes.sm', content)

    with open(filepath, 'w') as f:
        f.write(content)

files_to_process = [
    'app/src/main/kotlin/com/gymcoach/app/presentation/program/ProgramDetailScreen.kt',
    'app/src/main/kotlin/com/gymcoach/app/presentation/readiness/ReadinessScreen.kt',
    'app/src/main/kotlin/com/gymcoach/app/presentation/onboarding/OnboardingScreen.kt',
    'app/src/main/kotlin/com/gymcoach/app/presentation/history/WorkoutHistoryDetailScreen.kt',
    'app/src/main/kotlin/com/gymcoach/app/presentation/template/WorkoutTemplateScreen.kt',
    'app/src/main/kotlin/com/gymcoach/app/presentation/progress/ProgressionAnalyticsScreen.kt'
]

for file in files_to_process:
    process_file(file)
